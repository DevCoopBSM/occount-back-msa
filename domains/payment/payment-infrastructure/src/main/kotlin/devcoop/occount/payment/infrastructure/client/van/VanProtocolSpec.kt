package devcoop.occount.payment.infrastructure.client.van

import org.springframework.stereotype.Component

@Component
class VanProtocolSpec(
    properties: VanProperties,
) {
    private val protocol = properties.protocol

    val stxByte: Byte = parseSingleByte(protocol.stx, "stx")
    val etxByte: Byte = parseSingleByte(protocol.etx, "etx")
    val separatorByte: Byte = parseSingleByte(protocol.separator, "separator")
    val separatorChar: Char = (separatorByte.toInt() and 0xff).toChar()
    val recordSeparatorByte: Byte = parseSingleByte(protocol.recordSeparator, "recordSeparator")
    val recordSeparatorChar: Char = (recordSeparatorByte.toInt() and 0xff).toChar()
    val blankByte: Byte = parseSingleByte(protocol.blank, "blank")
    val ackByte: Byte = parseSingleByte(protocol.ack, "ack")
    val dleByte: Byte = parseSingleByte(protocol.dle, "dle")
    val formFeedByte: Byte = parseSingleByte(protocol.formFeed, "formFeed")
    val nakByte: Byte = parseSingleByte(protocol.nak, "nak")

    val ackBytes: ByteArray = repeatByte(ackByte)
    val dleHex: String = toHex(byteArrayOf(dleByte))
    val dleCompletedHex: String = toHex(repeatByte(dleByte))
    val formFeedHex: String = toHex(repeatByte(formFeedByte))
    val nakHex: String = toHex(repeatByte(nakByte))
    val transactionTimeoutNanos: Long = protocol.transactionTimeoutSeconds * 1_000_000_000L

    fun toHex(bytes: ByteArray): String {
        return bytes.joinToString(separator = "") { "%02x".format(it.toInt() and 0xff) }
    }

    private fun repeatByte(value: Byte): ByteArray {
        return ByteArray(SIGNAL_BYTE_COUNT) { value }
    }

    companion object {
        // VAN 프로토콜 신호 바이트는 3바이트 반복으로 전송
        private const val SIGNAL_BYTE_COUNT = 3
    }

    fun splitFrames(bytes: ByteArray): List<ByteArray> {
        val frames = mutableListOf<ByteArray>()
        var i = 0

        while (i < bytes.size) {
            val byte = bytes[i]
            if (byte == stxByte) {
                val buffer = mutableListOf<Byte>()
                buffer.add(byte)
                i++
                while (i < bytes.size) {
                    buffer.add(bytes[i])
                    if (bytes[i] == etxByte) {
                        i++
                        if (i < bytes.size) buffer.add(bytes[i])
                        break
                    }
                    i++
                }
                frames.add(buffer.toByteArray())
                i++
            } else {
                val buffer = mutableListOf<Byte>()
                buffer.add(byte)
                i++
                while (i < bytes.size && bytes[i] == byte) {
                    buffer.add(bytes[i])
                    i++
                }
                frames.add(buffer.toByteArray())
            }
        }
        return frames
    }

    private fun parseSingleByte(value: String, fieldName: String): Byte {
        val normalized = value.removePrefix("0x").removePrefix("0X").lowercase()
        require(normalized.length == 2) {
            "van.api.protocol.$fieldName 는 1바이트 hex 값이어야 합니다."
        }

        return normalized.toInt(16).toByte()
    }
}
