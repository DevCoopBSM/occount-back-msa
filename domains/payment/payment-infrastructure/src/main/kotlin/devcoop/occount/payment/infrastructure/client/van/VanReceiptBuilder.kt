package devcoop.occount.payment.infrastructure.client.van

import devcoop.occount.payment.application.dto.request.ItemCommand
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

object VanReceiptBuilder {
    private const val maxProductNameLength = 8
    private const val lineLength = 58
    private val eucKr: Charset = Charset.forName("EUC-KR")

    fun buildReceiptLines(items: List<ItemCommand>): ByteArray {
        if (items.isEmpty()) {
            return ByteArray(0)
        }

        val out = ByteArrayOutputStream()
        items.forEachIndexed { index, item ->
            out.write(buildReceiptLine(index + 1, item))
        }
        return out.toByteArray()
    }

    private fun buildReceiptLine(index: Int, item: ItemCommand): ByteArray {
        val name = truncateByByteLength(item.name, maxProductNameLength * 2)
        val leftPart = String.format("%2d. %s", index, name).toByteArray(eucKr)
        val quantityPart = String.format("%4d개", item.quantity).toByteArray(eucKr)
        val amountPart = String.format("%10s원", "%,d".format(item.total)).toByteArray(eucKr)

        val leftPaddingSize = (26 - leftPart.size).coerceAtLeast(0)

        return leftPart +
            ByteArray(leftPaddingSize) { ' '.code.toByte() } +
            quantityPart +
            ByteArray(4) { ' '.code.toByte() } +
            amountPart +
            byteArrayOf('\n'.code.toByte())
    }

    private fun truncateByByteLength(value: String, maxBytes: Int): String {
        val builder = StringBuilder()
        var currentBytes = 0

        value.forEach { character ->
            val encoded = character.toString().toByteArray(eucKr)
            if (currentBytes + encoded.size > maxBytes) {
                return builder.toString()
            }
            builder.append(character)
            currentBytes += encoded.size
        }

        return builder.toString()
    }
}
