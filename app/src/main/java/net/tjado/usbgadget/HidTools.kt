package net.tjado.usbgadget

import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import java.io.DataInputStream
import java.io.FileDescriptor
import java.io.IOException
import kotlin.concurrent.thread

object KeyboardTest {
    // Avoids y and z, which swap on QWERTZ layouts, and has no Enter, so it can't submit anything
    const val TEST_TEXT = "USB Gadget Tool test 123"

    const val RESULT_OK = "KEYBOARD_TEST_OK"
    private const val WATCHDOG_SECONDS = 8

    private val RELEASE = ByteArray(8)

    private const val US_UNSHIFTED = "1234567890"
    private const val US_SHIFTED = "!@#$%^&*()"
    private const val US_SYMBOLS = "-=[]\\;'`,./"
    private const val US_SYMBOLS_SHIFTED = "_+{}|:\"~<>?"
    private val US_SYMBOL_USAGES = intArrayOf(
        0x2d, 0x2e, 0x2f, 0x30, 0x31, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38
    )

    /** The usage and whether it needs Shift on a US layout, or null if the key doesn't exist. */
    private fun keyFor(char: Char): Pair<Int, Boolean>? {
        return when (char) {
            in 'a'..'z' -> Pair(0x04 + (char - 'a'), false)
            in 'A'..'Z' -> Pair(0x04 + (char - 'A'), true)
            in US_UNSHIFTED -> Pair(0x1e + US_UNSHIFTED.indexOf(char), false)
            in US_SHIFTED -> Pair(0x1e + US_SHIFTED.indexOf(char), true)
            in US_SYMBOLS -> Pair(US_SYMBOL_USAGES[US_SYMBOLS.indexOf(char)], false)
            in US_SYMBOLS_SHIFTED -> Pair(US_SYMBOL_USAGES[US_SYMBOLS_SHIFTED.indexOf(char)], true)
            ' ' -> Pair(0x2c, false)
            '\n' -> Pair(0x28, false)
            '\t' -> Pair(0x2b, false)
            else -> null
        }
    }

    private fun keyPress(usage: Int, shift: Boolean = false): List<ByteArray> {
        val modifier = if (shift) MODIFIER_LEFT_SHIFT else 0
        return listOf(byteArrayOf(modifier.toByte(), 0, usage.toByte(), 0, 0, 0, 0, 0), RELEASE)
    }

    /** Boot keyboard reports for [text], for a host set to a US layout. Skips unknown characters. */
    fun reportsFor(text: String): List<ByteArray> {
        return text.mapNotNull { keyFor(it) }.flatMap { (usage, shift) -> keyPress(usage, shift) }
    }

    /** The reports that turn [old] into [new] on the host: Backspaces, then the new characters. */
    fun reportsForEdit(old: String, new: String): List<ByteArray> {
        val common = old.commonPrefixWith(new).length
        val backspaces = List(old.length - common) { keyPress(USAGE_BACKSPACE) }.flatten()
        return backspaces + reportsFor(new.substring(common))
    }

    /**
     * A write to a HID gadget blocks while the host isn't reading, so a watchdog ends the shell.
     * Otherwise leftover keystrokes could reach the host much later.
     */
    fun typeScript(node: String, reports: List<ByteArray>): String {
        val writes = reports.joinToString("\n") { report ->
            writeCommand(node, report) + " || { kill \$WATCHDOG; exit 1; }"
        }
        return "( sleep $WATCHDOG_SECONDS; kill -9 $$ ) >/dev/null 2>&1 &\n" +
            "WATCHDOG=$!\n" +
            "$writes\n" +
            "kill \$WATCHDOG\n" +
            "echo $RESULT_OK\n"
    }

    fun writeCommand(node: String, report: ByteArray): String {
        val escaped = report.joinToString("") { "\\x%02x".format(it) }
        return "echo -ne '$escaped' > $node"
    }

    fun releaseScript(node: String): String {
        return typeScript(node, listOf(RELEASE))
    }

    private const val MODIFIER_LEFT_SHIFT = 0x02
    private const val USAGE_BACKSPACE = 0x2a
}

/**
 * Lets this app open a gadget's HID nodes without root. Only the app gets access, because any app
 * that can write to a keyboard node can type on the connected computer. ueventd recreates the
 * nodes on every activation, so this has to run after each one. The SELinux part needs
 * magiskpolicy, and without it opening the node fails and [KeyboardTyper] uses a root shell.
 */
object HidNodeAccess {
    private const val LABEL = "usbgadget_hidg"

    fun script(gadgetPath: String, appUid: Int): String {
        return "POLICY=0\n" +
            "magiskpolicy --live 'type $LABEL' 'typeattribute $LABEL mlstrustedobject' " +
            "'typeattribute $LABEL dev_type' 'allow $LABEL device filesystem associate' " +
            "'allow $LABEL tmpfs filesystem associate' " +
            "'allow untrusted_app_all $LABEL chr_file { open read write getattr ioctl }' " +
            ">/dev/null 2>&1 && POLICY=1\n" +
            "for f in $gadgetPath/functions/hid.*; do\n" +
            "ls -d $gadgetPath/configs/*/\$(basename \$f) >/dev/null 2>&1 || continue\n" +
            "n=/dev/hidg\$(cut -d: -f2 \$f/dev)\n" +
            "i=0; while [ ! -e \$n ] && [ \$i -lt 20 ]; do sleep 0.1; i=\$((i+1)); done\n" +
            "chown $appUid \$n 2>/dev/null\n" +
            "chmod 600 \$n 2>/dev/null\n" +
            "[ \$POLICY = 1 ] && chcon u:object_r:$LABEL:s0 \$n 2>/dev/null\n" +
            "done\n"
    }
}

/**
 * Sends typed keys to the host in order. It writes to the node directly when the app has access,
 * and otherwise through one root shell that stays open. If the host stops reading, the keys still
 * waiting are dropped, so that they can't reach the host much later.
 */
class KeyboardTyper(private val node: String, private val onFailed: () -> Unit) {
    private val pending = ArrayList<ByteArray>()
    private var running = false
    private var shell: Process? = null
    private var shellPid: String? = null
    private var directNode: FileDescriptor? = null
    private var directUnavailable = false

    fun send(reports: List<ByteArray>) {
        if (reports.isEmpty()) {
            return
        }
        synchronized(pending) {
            pending.addAll(reports)
            if (running) {
                return
            }
            running = true
        }
        thread(name = "keyboard-typer") { drain() }
    }

    /** Ends the shell. It would also end by itself once the app is gone and its input closes. */
    fun close() {
        thread(name = "keyboard-typer-close") {
            synchronized(pending) {
                pending.clear()
                shell?.outputStream?.close()
                shell = null
                closeDirect()
            }
        }
    }

    private fun drain() {
        while (true) {
            val batch = synchronized(pending) {
                if (pending.isEmpty()) {
                    running = false
                    return
                }
                ArrayList(pending).also { pending.clear() }
            }
            if (!write(batch)) {
                synchronized(pending) { pending.clear() }
                killShell()
                closeDirect()
                onFailed()
            }
        }
    }

    private fun write(reports: List<ByteArray>): Boolean {
        val node = directNode ?: openDirect()
        return if (node != null) writeDirect(node, reports) else writeThroughShell(reports)
    }

    private fun openDirect(): FileDescriptor? {
        if (directUnavailable) {
            return null
        }
        return try {
            Os.open(node, OsConstants.O_WRONLY or OsConstants.O_NONBLOCK, 0).also { directNode = it }
        } catch (e: ErrnoException) {
            Log.i("KeyboardTyper", "Using a root shell, as $node can't be opened: ${e.message}")
            directUnavailable = true
            null
        }
    }

    private fun closeDirect() {
        val node = directNode ?: return
        directNode = null
        try {
            Os.close(node)
        } catch (e: ErrnoException) {
            Log.w("KeyboardTyper", "Closing failed: ${e.message}")
        }
    }

    /** The node is non-blocking, so a write fails with EAGAIN while the last report is unread. */
    private fun writeDirect(node: FileDescriptor, reports: List<ByteArray>): Boolean {
        return try {
            reports.all { report -> writeReport(node, report) }
        } catch (e: ErrnoException) {
            Log.w("KeyboardTyper", "Writing failed: ${e.message}")
            false
        }
    }

    private fun writeReport(node: FileDescriptor, report: ByteArray): Boolean {
        while (true) {
            try {
                Os.write(node, report, 0, report.size)
                return true
            } catch (e: ErrnoException) {
                if (e.errno != OsConstants.EAGAIN) {
                    throw e
                }
            }
            val poll = StructPollfd()
            poll.fd = node
            poll.events = OsConstants.POLLOUT.toShort()
            if (Os.poll(arrayOf(poll), TIMEOUT_MS.toInt()) == 0) {
                return false
            }
        }
    }

    private fun writeThroughShell(reports: List<ByteArray>): Boolean {
        return try {
            val process = shell ?: startShell()
            val lines = reports.joinToString("") { KeyboardTest.writeCommand(node, it) + " || echo $RESULT_FAILED\n" }
            process.outputStream.write((lines + "echo $RESULT_DONE\n").toByteArray())
            process.outputStream.flush()
            readResult(process) == RESULT_DONE
        } catch (e: IOException) {
            false
        }
    }

    private fun startShell(): Process {
        val process = ProcessBuilder("su", "-").redirectErrorStream(true).start()
        shell = process
        process.outputStream.write("echo $PID_PREFIX$$\n".toByteArray())
        process.outputStream.flush()
        val line = readLine(process) ?: throw IOException("No root shell")
        shellPid = line.removePrefix(PID_PREFIX)
        return process
    }

    /** Returns [RESULT_DONE], or [RESULT_FAILED] if a write failed, or null if the host stalled. */
    private fun readResult(process: Process): String? {
        var failed = false
        while (true) {
            when (readLine(process) ?: return null) {
                RESULT_FAILED -> failed = true
                RESULT_DONE -> return if (failed) RESULT_FAILED else RESULT_DONE
            }
        }
    }

    private fun readLine(process: Process): String? {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        val line = StringBuilder()
        while (System.currentTimeMillis() < deadline) {
            if (process.inputStream.available() == 0) {
                Thread.sleep(POLL_MS)
                continue
            }
            val byte = process.inputStream.read()
            if (byte < 0) {
                return null
            }
            if (byte == '\n'.code) {
                return line.toString()
            }
            line.append(byte.toChar())
        }
        return null
    }

    private fun killShell() {
        val process = shell ?: return
        shell = null
        shellPid?.let { ExecuteAsRootUtil.execute("kill -9 $it\n") }
        shellPid = null
        process.destroy()
    }

    private companion object {
        const val PID_PREFIX = "KEYBOARD_TYPER_PID="
        const val RESULT_DONE = "KEYBOARD_TYPER_DONE"
        const val RESULT_FAILED = "KEYBOARD_TYPER_FAILED"
        const val TIMEOUT_MS = 5000L
        const val POLL_MS = 2L
    }
}

/**
 * Connects a [CtapAuthenticator] to a /dev/hidgN node. The app can't open the node itself, so root
 * dd processes carry the reports. dd keeps each report in one write, which f_hid requires.
 */
class CtapHidSession(
    private val node: String,
    private val onEvent: (String) -> Unit,
    private val onStopped: () -> Unit
) {
    private var reader: Process? = null
    private var writer: Process? = null

    @Volatile
    private var stopping = false

    fun start() {
        thread(name = "ctap-hid-session") { run() }
    }

    fun stop() {
        stopping = true
        thread(name = "ctap-hid-stop") { killReaders() }
    }

    private fun run() {
        try {
            // A previous session may be left over if the app was killed
            killReaders()
            val size = CtapAuthenticator.REPORT_SIZE
            val input = su("exec dd if=$node bs=$size 2>/dev/null")
            val output = su("exec dd of=$node bs=$size conv=notrunc 2>/dev/null")
            reader = input
            writer = output

            val authenticator = CtapAuthenticator(onEvent)
            val stream = DataInputStream(input.inputStream)
            val report = ByteArray(size)
            onEvent("Test authenticator is running on $node")
            while (true) {
                stream.readFully(report)
                for (reply in authenticator.handleReport(report)) {
                    output.outputStream.write(reply)
                    output.outputStream.flush()
                }
            }
        } catch (e: IOException) {
            if (!stopping) {
                onEvent("The connection to $node ended: ${e.message}")
            }
        } finally {
            reader?.destroy()
            writer?.destroy()
            onEvent("Test authenticator stopped. Its test credentials are gone.")
            onStopped()
        }
    }

    private fun su(command: String): Process {
        return ProcessBuilder("su", "-c", command).start()
    }

    private fun killReaders() {
        ExecuteAsRootUtil.execute("pkill -f 'dd if=$node' 2>/dev/null\npkill -f 'dd of=$node' 2>/dev/null\n")
    }
}
