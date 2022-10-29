/**
 * Authorizer
 *
 * Copyright 2016 by Tjado Mäcke <tjado></tjado>@maecke.de>
 * Licensed under GNU General Public License 3.0.
 *
 * @license GPL-3.0 <https:></https:>//opensource.org/licenses/GPL-3.0>
 */
package net.tjado.usbgadget

import androidx.core.util.Pair
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.lang.Exception
import java.util.*

/*
 * Class by muzikant <http://stackoverflow.com/users/624109/muzikant>
 *
 * Reference:
 * http://muzikant-android.blogspot.com/2011/02/how-to-get-root-access-and-execute.html
 * http://stackoverflow.com/a/7102780
 */
object ExecuteAsRootUtil {
    fun canRunRootCommands(): Boolean {
        var retval = false
        val suProcess: Process
        try {
            suProcess = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(suProcess.outputStream)
            val osRes = DataInputStream(suProcess.inputStream)

            if (null != os && null != osRes) {
                // Getting the id of the current user to check if this is root
                os.writeBytes("id\n")
                os.flush()
                val currUid = osRes.readLine()
                val exitSu: Boolean

                if (null == currUid) {
                    retval = false
                    exitSu = false
                    Log.d("ROOT", "Can't get root access or denied by user")
                } else if (currUid.contains("uid=0")) {
                    retval = true
                    exitSu = true
                    Log.d("ROOT", "Root access granted")
                } else {
                    retval = false
                    exitSu = true
                    Log.d("ROOT", "Root access rejected: $currUid")
                }

                if (exitSu) {
                    os.writeBytes("\nexit\n")
                    os.flush()
                }
            }
        } catch (e: Exception) {
            // Can't get root !
            // Probably broken pipe exception on trying to write to output stream (os) after su failed, meaning that the device is not rooted
            retval = false
            Log.d("ROOT", "Root access rejected [" + e.javaClass.name + "] : " + e.message)
        }

        return retval
    }

    fun execute(command: String): Pair<*, *> {
        return execute(arrayOf(command))
    }

    fun execute(commands: Array<String>?): Pair<*, *> {
        var retval = false
        var output: String? = null

        try {
            if (commands != null && commands.isNotEmpty()) {
                val suProcess = Runtime.getRuntime().exec("su -")
                val stdin = DataOutputStream(suProcess.outputStream)
                val stdout = suProcess.inputStream
                val stderr = suProcess.errorStream
                for (cmd in commands) {
                    Log.d("ROOT", String.format("Execute command: %s", cmd))
                    stdin.writeBytes(cmd)
                    stdin.flush()
                }
                stdin.writeBytes("\nexit\n")
                stdin.flush()
                stdin.close()
                val sbOut = StringBuffer()
                val scanner = Scanner(stdout)
                while (scanner.hasNext()) {
                    sbOut.append(scanner.nextLine())
                    sbOut.append(System.lineSeparator())
                }
                val sbErr = StringBuffer()
                val scannerErr = Scanner(stderr)
                while (scannerErr.hasNext()) {
                    sbErr.append(scannerErr.nextLine())
                    if (scannerErr.hasNext()) sbErr.append(System.lineSeparator())
                }
                output = sbOut.toString()
                Log.d("ROOT (stdout)", output)
                val error = sbErr.toString()
                Log.d("ROOT (stderr)", error)
                retval = error != "Permission denied"
            }
        } catch (ex: IOException) {
            Log.w("ROOT", "Can't get root access", ex)
        } catch (ex: SecurityException) {
            Log.w("ROOT", "Can't get root access", ex)
        } catch (ex: Exception) {
            Log.w("ROOT", "Error executing internal operation", ex)
        }

        return Pair<Any?, Any?>(retval, output)
    }
}