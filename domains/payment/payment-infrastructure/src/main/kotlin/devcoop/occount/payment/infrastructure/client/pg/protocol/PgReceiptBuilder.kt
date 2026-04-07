package devcoop.occount.payment.infrastructure.client.pg.protocol

import devcoop.occount.payment.infrastructure.client.pg.model.PgProduct
import java.nio.charset.Charset

object PgReceiptBuilder {
    private const val MAX_PRODUCT_NAME_LENGTH = 8
    private const val RECEIPT_LINE_LENGTH = 58
    private val eucKr: Charset = Charset.forName("EUC-KR")

    fun buildReceiptLines(products: List<PgProduct>): ByteArray {
        if (products.isEmpty()) {
            return byteArrayOf()
        }

        return buildList {
            products.forEachIndexed { index, product ->
                val name = truncateName(product.name)
                val leftPart = String.format("%2d. %s", index + 1, name)
                val quantityPart = String.format("%4d개", product.quantity)
                val amountPart = String.format("%10s원", "%,d".format(product.total))

                val leftBytes = leftPart.toByteArray(eucKr)
                val quantityBytes = quantityPart.toByteArray(eucKr)
                val amountBytes = amountPart.toByteArray(eucKr)

                val spaceCount1 = 26 - leftBytes.size
                val spaceCount2 = 4

                addAll(leftBytes.toList())
                repeat(spaceCount1) { add(SPACE) }
                addAll(quantityBytes.toList())
                repeat(spaceCount2) { add(SPACE) }
                addAll(amountBytes.toList())
                add(LINE_FEED)
            }
        }.toByteArray()
    }

    fun calculateReceiptLength(products: List<PgProduct>): Int {
        return products.size * RECEIPT_LINE_LENGTH
    }

    private fun truncateName(name: String): String {
        val encoded = name.toByteArray(eucKr)
        val maxBytes = MAX_PRODUCT_NAME_LENGTH * 2
        if (encoded.size <= maxBytes) {
            return name
        }

        return encoded.copyOf(maxBytes).toString(eucKr).trimEnd('\uFFFD')
    }

    private const val SPACE: Byte = 0x20
    private const val LINE_FEED: Byte = 0x0A
}
