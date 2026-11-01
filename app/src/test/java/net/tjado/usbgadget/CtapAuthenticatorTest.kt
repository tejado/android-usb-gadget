package net.tjado.usbgadget

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec

class CtapAuthenticatorTest {
    private val authenticator = CtapAuthenticator()
    private val hash = ByteArray(32) { 7 }

    private fun send(cid: Int, command: Int, payload: ByteArray): Pair<Int, ByteArray> {
        val replies = ArrayList<ByteArray>()
        var offset = minOf(payload.size, 57)
        replies += authenticator.handleReport(
            ByteBuffer.allocate(64).putInt(cid).put((command or 0x80).toByte())
                .putShort(payload.size.toShort()).put(payload, 0, offset).array()
        )
        var sequence = 0
        while (offset < payload.size) {
            val chunk = minOf(payload.size - offset, 59)
            replies += authenticator.handleReport(
                ByteBuffer.allocate(64).putInt(cid).put((sequence++).toByte())
                    .put(payload, offset, chunk).array()
            )
            offset += chunk
        }
        return decode(cid, replies)
    }

    private fun decode(cid: Int, replies: List<ByteArray>): Pair<Int, ByteArray> {
        replies.forEach { assertEquals(64, it.size) }
        val first = ByteBuffer.wrap(replies[0])
        assertEquals(cid, first.int)
        val command = first.get().toInt() and 0x7f
        val length = first.short.toInt() and 0xffff
        val message = replies[0].copyOfRange(7, 64).toMutableList()
        replies.drop(1).forEachIndexed { index, report ->
            assertEquals(index.toByte(), report[4])
            message += report.copyOfRange(5, 64).toList()
        }
        return Pair(command, message.take(length).toByteArray())
    }

    private fun openChannel(): Int {
        val nonce = ByteArray(8) { it.toByte() }
        val (command, reply) = send(-1, 0x06, nonce)
        assertEquals(0x06, command)
        assertArrayEquals(nonce, reply.copyOfRange(0, 8))
        assertEquals(0x0c, reply[16].toInt())
        return ByteBuffer.wrap(reply, 8, 4).int
    }

    private fun cbor(cid: Int, ctapCommand: Int, parameters: Map<Any, Any>): Pair<Int, Map<*, *>> {
        val (command, reply) = send(cid, 0x10, byteArrayOf(ctapCommand.toByte()) + Cbor.encode(parameters))
        assertEquals(0x10, command)
        return Pair(reply[0].toInt() and 0xff, Cbor.decodeMap(reply.copyOfRange(1, reply.size)))
    }

    private fun makeCredentialRequest(options: Map<Any, Any> = emptyMap()): Map<Any, Any> {
        return linkedMapOf(
            1L to hash,
            2L to linkedMapOf<Any, Any>("id" to "example.org"),
            3L to linkedMapOf<Any, Any>("id" to byteArrayOf(1)),
            4L to listOf(linkedMapOf<Any, Any>("alg" to -7L, "type" to "public-key")),
            7L to options
        )
    }

    @Test
    fun pingSurvivesFragmentation() {
        val cid = openChannel()
        val payload = ByteArray(1000) { it.toByte() }
        val (command, reply) = send(cid, 0x01, payload)
        assertEquals(0x01, command)
        assertArrayEquals(payload, reply)
    }

    @Test
    fun getInfoAdvertisesFido2WithoutResidentKeys() {
        val (status, info) = cbor(openChannel(), 0x04, emptyMap())
        assertEquals(0, status)
        assertEquals(listOf("FIDO_2_0"), info[1L])
        assertEquals(false, (info[4L] as Map<*, *>)["rk"])
    }

    @Test
    fun registrationAndSignInProduceAValidSignature() {
        val cid = openChannel()
        val (status, attestation) = cbor(cid, 0x01, makeCredentialRequest())
        assertEquals(0, status)
        assertEquals("packed", attestation[1L])

        val authData = attestation[2L] as ByteArray
        val rpIdHash = MessageDigest.getInstance("SHA-256").digest("example.org".toByteArray())
        assertArrayEquals(rpIdHash, authData.copyOfRange(0, 32))
        assertEquals(0x41, authData[32].toInt())

        val idLength = ByteBuffer.wrap(authData, 53, 2).short.toInt()
        val credentialId = authData.copyOfRange(55, 55 + idLength)
        val coseKey = Cbor.decodeMap(authData.copyOfRange(55 + idLength, authData.size))

        val (assertStatus, assertion) = cbor(
            cid, 0x02, linkedMapOf(
                1L to "example.org",
                2L to hash,
                3L to listOf(linkedMapOf<Any, Any>("id" to credentialId, "type" to "public-key"))
            )
        )
        assertEquals(0, assertStatus)
        val signedData = assertion[2L] as ByteArray
        assertEquals(0x01, signedData[32].toInt())
        assertEquals(1, ByteBuffer.wrap(signedData, 33, 4).int)

        val parameters = AlgorithmParameters.getInstance("EC")
        parameters.init(ECGenParameterSpec("secp256r1"))
        val publicKey = KeyFactory.getInstance("EC").generatePublic(
            ECPublicKeySpec(
                ECPoint(BigInteger(1, coseKey[-2L] as ByteArray), BigInteger(1, coseKey[-3L] as ByteArray)),
                parameters.getParameterSpec(ECParameterSpec::class.java)
            )
        )
        fun verifies(data: ByteArray, signature: ByteArray): Boolean {
            val verifier = Signature.getInstance("SHA256withECDSA")
            verifier.initVerify(publicKey)
            verifier.update(data)
            verifier.update(hash)
            return verifier.verify(signature)
        }
        assertTrue(verifies(authData, (attestation[3L] as Map<*, *>)["sig"] as ByteArray))
        assertTrue(verifies(signedData, assertion[3L] as ByteArray))
    }

    @Test
    fun unknownCredentialAndUnsupportedOptionsAreRefused() {
        val cid = openChannel()
        val unknown = linkedMapOf<Any, Any>(
            1L to "example.org",
            2L to hash,
            3L to listOf(linkedMapOf<Any, Any>("id" to ByteArray(32), "type" to "public-key"))
        )
        assertEquals(0x2e, cbor(cid, 0x02, unknown).first)
        assertEquals(0x2b, cbor(cid, 0x01, makeCredentialRequest(linkedMapOf("rk" to true))).first)
    }

    @Test
    fun keyboardReportsCoverSymbolsAndControlKeys() {
        fun key(char: Char) = KeyboardTest.reportsFor(char.toString())[0].let { Pair(it[0].toInt(), it[2].toInt()) }
        assertEquals(Pair(2, 0x1e), key('!'))
        assertEquals(Pair(2, 0x1f), key('@'))
        assertEquals(Pair(0, 0x27), key('0'))
        assertEquals(Pair(2, 0x27), key(')'))
        assertEquals(Pair(0, 0x2d), key('-'))
        assertEquals(Pair(2, 0x2d), key('_'))
        assertEquals(Pair(0, 0x34), key('\''))
        assertEquals(Pair(2, 0x34), key('"'))
        assertEquals(Pair(0, 0x38), key('/'))
        assertEquals(Pair(2, 0x38), key('?'))
        assertEquals(Pair(0, 0x28), key('\n'))
        assertTrue(KeyboardTest.reportsFor("ä").isEmpty())
    }

    @Test
    fun editsSendBackspacesAndOnlyTheChangedText() {
        assertEquals(2, KeyboardTest.reportsForEdit("ab", "abc").size)
        assertEquals(0x06, KeyboardTest.reportsForEdit("ab", "abc")[0][2].toInt())
        val correction = KeyboardTest.reportsForEdit("abc", "abd")
        assertEquals(4, correction.size)
        assertEquals(0x2a, correction[0][2].toInt())
        assertEquals(0x07, correction[2][2].toInt())
    }

    @Test
    fun keyboardReportsUseShiftOnlyForUppercase() {
        val reports = KeyboardTest.reportsFor("Ab1")
        assertArrayEquals(byteArrayOf(2, 0, 0x04, 0, 0, 0, 0, 0), reports[0])
        assertArrayEquals(ByteArray(8), reports[1])
        assertArrayEquals(byteArrayOf(0, 0, 0x05, 0, 0, 0, 0, 0), reports[2])
        assertArrayEquals(byteArrayOf(0, 0, 0x1e, 0, 0, 0, 0, 0), reports[4])
    }
}
