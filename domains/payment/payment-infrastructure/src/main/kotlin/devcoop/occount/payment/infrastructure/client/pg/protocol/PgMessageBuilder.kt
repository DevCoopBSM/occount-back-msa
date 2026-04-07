package devcoop.occount.payment.infrastructure.client.pg.protocol

import devcoop.occount.payment.infrastructure.client.pg.model.PgProduct

object PgMessageBuilder {
    fun buildPaymentMessage(amount: Int, products: List<PgProduct>): ByteArray {
        val amountString = amount.toString()
        val productData = PgReceiptBuilder.buildReceiptLines(products)
        val length = calculatePaymentMessageLength(amountString, products).toString().padStart(4, '0')

        val data = mutableListOf<Byte>().apply {
            add(STX)
            addAll(length.encodeToByteArray().toList())
            addAll("0101".encodeToByteArray().toList())
            add(FS)
            addAll("01".encodeToByteArray().toList())
            add(FS)
            addAll(amountString.encodeToByteArray().toList())
            add(FS)
            add(FS)
            add(FS)
            addAll("00".encodeToByteArray().toList())
            add(FS)
            add(FS)
            addAll(productData.toList())
            add(FS)
            add(SPACE)
            add(FS)
            add(FS)
            add(ETX)
        }.toByteArray()

        return data + calculateBcc(data)
    }

    fun buildCancelMessage(
        amount: Int,
        approvalDate: String,
        approvalNumber: String,
        products: List<PgProduct>,
    ): ByteArray {
        val amountString = amount.toString()
        val productData = PgReceiptBuilder.buildReceiptLines(products)
        val length = calculatePaymentMessageLength(amountString, products).toString().padStart(4, '0')

        val data = mutableListOf<Byte>().apply {
            add(STX)
            addAll(length.encodeToByteArray().toList())
            addAll("2101".encodeToByteArray().toList())
            add(FS)
            addAll("01".encodeToByteArray().toList())
            add(FS)
            addAll(amountString.encodeToByteArray().toList())
            add(FS)
            add(FS)
            add(FS)
            addAll("00".encodeToByteArray().toList())
            add(FS)
            addAll(approvalNumber.encodeToByteArray().toList())
            add(FS)
            addAll(approvalDate.encodeToByteArray().toList())
            add(FS)
            add(FS)
            addAll(productData.toList())
            add(FS)
            add(SPACE)
            add(FS)
            add(FS)
            add(FS)
            add(ETX)
        }.toByteArray()

        return data + calculateBcc(data)
    }

    fun calculateBcc(data: ByteArray): Byte {
        var bcc = 0
        for (index in 1 until data.size) {
            val byte = data[index].toInt()
            bcc = bcc xor byte
            if (byte == ETX.toInt()) {
                break
            }
        }
        bcc = bcc or 0x20
        return bcc.toByte()
    }

    private fun calculatePaymentMessageLength(amount: String, products: List<PgProduct>): Int {
        val baseLength = 1 + 4 + 4 + 1 + 2 + 1 + amount.length + 3
        val endLength = 2 + 3 + 1 + 2 + 1 + 1
        return baseLength + PgReceiptBuilder.calculateReceiptLength(products) + endLength
    }

    private const val STX: Byte = 0x02
    private const val ETX: Byte = 0x03
    private const val FS: Byte = 0x1C
    private const val SPACE: Byte = 0x20
}
