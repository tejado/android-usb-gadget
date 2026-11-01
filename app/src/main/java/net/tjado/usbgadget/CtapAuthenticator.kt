package net.tjado.usbgadget

import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec

/**
 * Minimal CTAP2 authenticator over CTAPHID, used only to test a FIDO gadget end to end.
 *
 * It is deliberately not a security key: every request is approved without user interaction, and
 * keys live in memory, so credentials stop working once the object is gone.
 */
class CtapAuthenticator(private val onEvent: (String) -> Unit = {}) {

    private class Credential(val rpId: String, val keys: KeyPair, var counter: Int = 0)

    private val credentials = HashMap<String, Credential>()
    private val random = SecureRandom()

    private var nextChannel = 1
    private var channel = 0
    private var command = 0
    private var expected = 0
    private var sequence = 0
    private val message = ByteArrayOutputStream()

    /** Handles one report from the host and returns the reports to send back. */
    fun handleReport(report: ByteArray): List<ByteArray> {
        if (report.size != REPORT_SIZE) {
            return emptyList()
        }
        val buffer = ByteBuffer.wrap(report)
        val cid = buffer.int
        val first = buffer.get().toInt() and 0xff

        if (first and 0x80 == 0) {
            return handleContinuation(cid, first, report)
        }

        val cmd = first and 0x7f
        val length = buffer.short.toInt() and 0xffff

        if (cmd == HID_INIT) {
            // INIT is always allowed and aborts a message in progress on that channel
            if (cid == channel) {
                resetMessage()
            }
            return init(cid, report.copyOfRange(7, 7 + 8))
        }
        if (cid == 0 || cid == BROADCAST) {
            return error(cid, ERR_INVALID_CHANNEL)
        }
        if (channel != 0) {
            return if (cid == channel) {
                resetMessage()
                error(cid, ERR_INVALID_SEQ)
            } else {
                error(cid, ERR_CHANNEL_BUSY)
            }
        }
        if (length > MAX_MESSAGE) {
            return error(cid, ERR_INVALID_LEN)
        }

        channel = cid
        command = cmd
        expected = length
        sequence = 0
        message.write(report, 7, minOf(length, REPORT_SIZE - 7))
        return finishIfComplete()
    }

    private fun handleContinuation(cid: Int, seq: Int, report: ByteArray): List<ByteArray> {
        if (channel == 0 || cid != channel) {
            return emptyList()
        }
        if (seq != sequence) {
            resetMessage()
            return error(cid, ERR_INVALID_SEQ)
        }
        sequence++
        message.write(report, 5, minOf(expected - message.size(), REPORT_SIZE - 5))
        return finishIfComplete()
    }

    private fun finishIfComplete(): List<ByteArray> {
        if (message.size() < expected) {
            return emptyList()
        }
        val cid = channel
        val cmd = command
        val payload = message.toByteArray()
        resetMessage()

        return when (cmd) {
            HID_PING -> frames(cid, HID_PING, payload)
            HID_CBOR -> frames(cid, HID_CBOR, handleCbor(payload))
            HID_CANCEL -> emptyList()
            else -> error(cid, ERR_INVALID_CMD)
        }
    }

    private fun resetMessage() {
        channel = 0
        message.reset()
    }

    private fun init(cid: Int, nonce: ByteArray): List<ByteArray> {
        val assigned = if (cid == BROADCAST) nextChannel++ else cid
        if (cid == BROADCAST) {
            onEvent("The host found the authenticator")
        }
        val payload = ByteBuffer.allocate(17)
            .put(nonce)
            .putInt(assigned)
            .put(2) // CTAPHID protocol version
            .put(1).put(0).put(0) // device version
            .put((CAP_CBOR or CAP_NMSG).toByte())
            .array()
        return frames(cid, HID_INIT, payload)
    }

    private fun error(cid: Int, code: Int): List<ByteArray> {
        return frames(cid, HID_ERROR, byteArrayOf(code.toByte()))
    }

    private fun frames(cid: Int, cmd: Int, payload: ByteArray): List<ByteArray> {
        val result = ArrayList<ByteArray>()
        var offset = minOf(payload.size, REPORT_SIZE - 7)
        result.add(
            ByteBuffer.allocate(REPORT_SIZE)
                .putInt(cid)
                .put((cmd or 0x80).toByte())
                .putShort(payload.size.toShort())
                .put(payload, 0, offset)
                .array()
        )
        var seq = 0
        while (offset < payload.size) {
            val chunk = minOf(payload.size - offset, REPORT_SIZE - 5)
            result.add(
                ByteBuffer.allocate(REPORT_SIZE)
                    .putInt(cid)
                    .put((seq++).toByte())
                    .put(payload, offset, chunk)
                    .array()
            )
            offset += chunk
        }
        return result
    }

    private fun handleCbor(request: ByteArray): ByteArray {
        if (request.isEmpty()) {
            return byteArrayOf(CTAP_INVALID_LENGTH.toByte())
        }
        return try {
            val parameters = request.copyOfRange(1, request.size)
            when (request[0].toInt() and 0xff) {
                CMD_MAKE_CREDENTIAL -> makeCredential(Cbor.decodeMap(parameters))
                CMD_GET_ASSERTION -> getAssertion(Cbor.decodeMap(parameters))
                CMD_GET_INFO -> success(getInfo())
                CMD_RESET -> {
                    credentials.clear()
                    byteArrayOf(CTAP_OK.toByte())
                }
                else -> byteArrayOf(CTAP_INVALID_COMMAND.toByte())
            }
        } catch (e: CtapException) {
            byteArrayOf(e.status.toByte())
        } catch (e: Exception) {
            onEvent("Could not process a request: $e")
            byteArrayOf(CTAP_INVALID_CBOR.toByte())
        }
    }

    private fun success(response: Any): ByteArray {
        return byteArrayOf(CTAP_OK.toByte()) + Cbor.encode(response)
    }

    private fun getInfo(): Map<Any, Any> {
        return linkedMapOf(
            1L to listOf("FIDO_2_0"),
            3L to AAGUID,
            4L to linkedMapOf("rk" to false, "up" to true, "plat" to false),
            5L to MAX_MESSAGE.toLong()
        )
    }

    private fun makeCredential(request: Map<*, *>): ByteArray {
        val clientDataHash = request[1L] as? ByteArray ?: throw CtapException(CTAP_MISSING_PARAMETER)
        val rpId = (request[2L] as? Map<*, *>)?.get("id") as? String
            ?: throw CtapException(CTAP_MISSING_PARAMETER)
        val algorithms = request[4L] as? List<*> ?: throw CtapException(CTAP_MISSING_PARAMETER)
        if (algorithms.none { (it as? Map<*, *>)?.get("alg") == ALG_ES256 }) {
            throw CtapException(CTAP_UNSUPPORTED_ALGORITHM)
        }
        val options = request[7L] as? Map<*, *>
        if (options?.get("rk") == true || options?.get("uv") == true) {
            throw CtapException(CTAP_UNSUPPORTED_OPTION)
        }
        if (findCredential(rpId, request[5L] as? List<*>) != null) {
            throw CtapException(CTAP_CREDENTIAL_EXCLUDED)
        }

        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec("secp256r1"), random)
        val keys = generator.generateKeyPair()
        val credentialId = ByteArray(32).also { random.nextBytes(it) }
        credentials[credentialId.toHex()] = Credential(rpId, keys)

        val authData = ByteArrayOutputStream().apply {
            write(sha256(rpId))
            write(FLAG_USER_PRESENT or FLAG_ATTESTED)
            write(ByteArray(4))
            write(AAGUID)
            write(ByteBuffer.allocate(2).putShort(credentialId.size.toShort()).array())
            write(credentialId)
            write(Cbor.encode(coseKey(keys.public as ECPublicKey)))
        }.toByteArray()

        onEvent("Registration succeeded for $rpId")
        // Self attestation: signed with the credential's own key, as there is no attestation key
        val attestation = linkedMapOf<Any, Any>(
            "alg" to ALG_ES256,
            "sig" to sign(keys, authData, clientDataHash)
        )
        return success(linkedMapOf<Any, Any>(1L to "packed", 2L to authData, 3L to attestation))
    }

    private fun getAssertion(request: Map<*, *>): ByteArray {
        val rpId = request[1L] as? String ?: throw CtapException(CTAP_MISSING_PARAMETER)
        val clientDataHash = request[2L] as? ByteArray ?: throw CtapException(CTAP_MISSING_PARAMETER)
        val options = request[5L] as? Map<*, *>
        if (options?.get("rk") == true || options?.get("uv") == true) {
            throw CtapException(CTAP_UNSUPPORTED_OPTION)
        }
        val (credentialId, credential) = findCredential(rpId, request[3L] as? List<*>)
            ?: throw CtapException(CTAP_NO_CREDENTIALS)

        // Browsers probe with up=false to check for a credential without asking for a touch
        val userPresent = options?.get("up") != false
        credential.counter++
        val authData = ByteBuffer.allocate(37)
            .put(sha256(rpId))
            .put((if (userPresent) FLAG_USER_PRESENT else 0).toByte())
            .putInt(credential.counter)
            .array()
        val signature = sign(credential.keys, authData, clientDataHash)

        if (userPresent) {
            onEvent("Sign-in succeeded for $rpId. The WebAuthn test passed.")
        }
        return success(
            linkedMapOf<Any, Any>(
                1L to linkedMapOf<Any, Any>("id" to credentialId, "type" to "public-key"),
                2L to authData,
                3L to signature
            )
        )
    }

    private fun sign(keys: KeyPair, authData: ByteArray, clientDataHash: ByteArray): ByteArray {
        val signer = Signature.getInstance("SHA256withECDSA")
        signer.initSign(keys.private, random)
        signer.update(authData)
        signer.update(clientDataHash)
        return signer.sign()
    }

    private fun findCredential(rpId: String, list: List<*>?): Pair<ByteArray, Credential>? {
        for (entry in list.orEmpty()) {
            val id = (entry as? Map<*, *>)?.get("id") as? ByteArray ?: continue
            val credential = credentials[id.toHex()] ?: continue
            if (credential.rpId == rpId) {
                return Pair(id, credential)
            }
        }
        return null
    }

    private fun coseKey(key: ECPublicKey): Map<Any, Any> {
        return linkedMapOf(
            1L to 2L, // kty: EC2
            3L to ALG_ES256,
            -1L to 1L, // crv: P-256
            -2L to key.w.affineX.toFixedBytes(),
            -3L to key.w.affineY.toFixedBytes()
        )
    }

    private fun BigInteger.toFixedBytes(): ByteArray {
        val bytes = toByteArray()
        return ByteArray(32).also {
            val length = minOf(bytes.size, 32)
            bytes.copyInto(it, 32 - length, bytes.size - length, bytes.size)
        }
    }

    private fun sha256(text: String): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
    }

    private fun ByteArray.toHex(): String {
        return joinToString("") { "%02x".format(it) }
    }

    private class CtapException(val status: Int) : Exception()

    companion object {
        const val REPORT_SIZE = 64

        private const val BROADCAST = -1
        private const val MAX_MESSAGE = 7609
        private const val ALG_ES256 = -7L
        private val AAGUID = ByteArray(16)

        private const val HID_PING = 0x01
        private const val HID_INIT = 0x06
        private const val HID_CBOR = 0x10
        private const val HID_CANCEL = 0x11
        private const val HID_ERROR = 0x3f

        private const val CAP_CBOR = 0x04
        private const val CAP_NMSG = 0x08

        private const val ERR_INVALID_CMD = 0x01
        private const val ERR_INVALID_LEN = 0x03
        private const val ERR_INVALID_SEQ = 0x04
        private const val ERR_CHANNEL_BUSY = 0x06
        private const val ERR_INVALID_CHANNEL = 0x0b

        private const val CMD_MAKE_CREDENTIAL = 0x01
        private const val CMD_GET_ASSERTION = 0x02
        private const val CMD_GET_INFO = 0x04
        private const val CMD_RESET = 0x07

        private const val CTAP_OK = 0x00
        private const val CTAP_INVALID_COMMAND = 0x01
        private const val CTAP_INVALID_LENGTH = 0x03
        private const val CTAP_INVALID_CBOR = 0x12
        private const val CTAP_MISSING_PARAMETER = 0x14
        private const val CTAP_CREDENTIAL_EXCLUDED = 0x19
        private const val CTAP_UNSUPPORTED_ALGORITHM = 0x26
        private const val CTAP_UNSUPPORTED_OPTION = 0x2b
        private const val CTAP_NO_CREDENTIALS = 0x2e

        private const val FLAG_USER_PRESENT = 0x01
        private const val FLAG_ATTESTED = 0x40
    }
}

/** The subset of CBOR that CTAP2 uses. Integers decode to Long and maps keep their order. */
object Cbor {
    fun encode(value: Any): ByteArray {
        val out = ByteArrayOutputStream()
        write(out, value)
        return out.toByteArray()
    }

    fun decodeMap(data: ByteArray): Map<*, *> {
        if (data.isEmpty()) {
            return emptyMap<Any, Any>()
        }
        return Reader(data).read() as Map<*, *>
    }

    private fun write(out: ByteArrayOutputStream, value: Any) {
        when (value) {
            is Boolean -> out.write(if (value) 0xf5 else 0xf4)
            is Long -> if (value >= 0) writeHead(out, 0, value) else writeHead(out, 1, -1 - value)
            is ByteArray -> {
                writeHead(out, 2, value.size.toLong())
                out.write(value)
            }
            is String -> {
                val bytes = value.toByteArray()
                writeHead(out, 3, bytes.size.toLong())
                out.write(bytes)
            }
            is List<*> -> {
                writeHead(out, 4, value.size.toLong())
                value.forEach { write(out, it!!) }
            }
            is Map<*, *> -> {
                writeHead(out, 5, value.size.toLong())
                value.forEach { (key, item) ->
                    write(out, key!!)
                    write(out, item!!)
                }
            }
            else -> throw IllegalArgumentException("Unsupported CBOR type")
        }
    }

    private fun writeHead(out: ByteArrayOutputStream, major: Int, value: Long) {
        val type = major shl 5
        when {
            value < 24 -> out.write(type or value.toInt())
            value < 0x100 -> {
                out.write(type or 24)
                out.write(value.toInt())
            }
            value < 0x10000 -> {
                out.write(type or 25)
                out.write(ByteBuffer.allocate(2).putShort(value.toShort()).array())
            }
            else -> {
                out.write(type or 26)
                out.write(ByteBuffer.allocate(4).putInt(value.toInt()).array())
            }
        }
    }

    private class Reader(data: ByteArray) {
        private val buffer = ByteBuffer.wrap(data)

        fun read(): Any {
            val head = buffer.get().toInt() and 0xff
            val major = head shr 5
            if (major == 7) {
                return when (head) {
                    0xf4 -> false
                    0xf5 -> true
                    else -> throw IllegalArgumentException("Unsupported CBOR value")
                }
            }
            val value = readLength(head and 0x1f)
            return when (major) {
                0 -> value
                1 -> -1 - value
                2 -> bytes(value)
                3 -> String(bytes(value))
                4 -> List(value.toInt()) { read() }
                5 -> LinkedHashMap<Any, Any>().also { map ->
                    repeat(value.toInt()) { map[read()] = read() }
                }
                else -> throw IllegalArgumentException("Unsupported CBOR type")
            }
        }

        private fun readLength(info: Int): Long {
            return when (info) {
                in 0..23 -> info.toLong()
                24 -> (buffer.get().toInt() and 0xff).toLong()
                25 -> (buffer.short.toInt() and 0xffff).toLong()
                26 -> buffer.int.toLong() and 0xffffffffL
                27 -> buffer.long
                else -> throw IllegalArgumentException("Unsupported CBOR length")
            }
        }

        private fun bytes(length: Long): ByteArray {
            return ByteArray(length.toInt()).also { buffer.get(it) }
        }
    }
}
