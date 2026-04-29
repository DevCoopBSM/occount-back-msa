package devcoop.occount.payment.infrastructure.client.van

import devcoop.occount.payment.application.dto.request.ItemCommand
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream

@Component
class VanMessageBuilder(
    private val protocolSpec: VanProtocolSpec,
    properties: VanProperties,
) {
    private val message = properties.message

    fun buildPaymentMessage(amount: Int, items: List<ItemCommand>): ByteArray {
        val amountString = amount.toString()
        val productData = VanReceiptBuilder.buildReceiptLines(items)

        return buildMessage(
            serviceType = message.paymentServiceType,
            bodyWriter = {
                writeAscii(message.transactionType)
                write(protocolSpec.separatorByte.toInt())
                writeAscii(amountString)
                write(protocolSpec.separatorByte.toInt())
                write(protocolSpec.separatorByte.toInt())
                write(protocolSpec.separatorByte.toInt())
                writeAscii(message.installmentMonths)
                write(protocolSpec.separatorByte.toInt())
                write(protocolSpec.separatorByte.toInt())
                write(productData)
                write(protocolSpec.separatorByte.toInt())
                write(protocolSpec.blankByte.toInt())
                write(protocolSpec.separatorByte.toInt())
                write(protocolSpec.separatorByte.toInt())
            },
        )
    }

    fun buildRefundMessage(amount: Int, approvalDate: String, approvalNumber: String): ByteArray {
        val amountString = amount.toString()

        return buildMessage(
            serviceType = message.refundServiceType,
            bodyWriter = {
                writeAscii(message.transactionType)
                write(protocolSpec.separatorByte.toInt())
                writeAscii(amountString)
                write(protocolSpec.separatorByte.toInt())
                write(protocolSpec.separatorByte.toInt())
                write(protocolSpec.separatorByte.toInt())
                writeAscii(message.installmentMonths)
                write(protocolSpec.separatorByte.toInt())
                writeAscii(approvalNumber)
                write(protocolSpec.separatorByte.toInt())
                writeAscii(approvalDate)
                write(protocolSpec.separatorByte.toInt())
                write(protocolSpec.separatorByte.toInt())
                write(protocolSpec.separatorByte.toInt())
                write(protocolSpec.blankByte.toInt())
                write(protocolSpec.separatorByte.toInt())
                write(protocolSpec.separatorByte.toInt())
                write(protocolSpec.separatorByte.toInt())
            },
        )
    }

    fun buildCardlessCancelMessage(
        amount: Int,
        approvalDate: String,
        terminalSequence: String,
        approvalNumber: String,
    ): ByteArray {
        val amountString = amount.toString()
        val paddedApprovalNumber = formatApprovalNumber(approvalNumber)
        val filler1 = buildString {
            append(CARDLESS_CANCEL_TAG)
            append(CARDLESS_CANCEL_VALUE_LENGTH)
            append(CARDLESS_CANCEL_CODE)
            append(approvalDate)
            append(terminalSequence)
            append(paddedApprovalNumber)
        }

        return buildMessage(
            serviceType = message.refundServiceType,
            bodyWriter = {
                writeAscii(amountString)
                write(protocolSpec.separatorByte.toInt())
                writeAscii(approvalDate)
                write(protocolSpec.separatorByte.toInt())
                writeAscii(paddedApprovalNumber)
                write(protocolSpec.separatorByte.toInt())
                writeAscii(filler1)
            },
        )
    }

    private fun formatApprovalNumber(approvalNumber: String): String {
        require(approvalNumber.length <= APPROVAL_NUMBER_LENGTH) {
            "승인번호는 12자를 넘을 수 없습니다."
        }
        return approvalNumber.padEnd(APPROVAL_NUMBER_LENGTH, ' ')
    }

    fun buildTerminalCloseMessage(): ByteArray {
        return buildMessage(
            serviceType = message.terminalCloseServiceType,
            bodyWriter = {
                writeAscii(message.terminalCloseFiller)
                write(protocolSpec.separatorByte.toInt())
                write(protocolSpec.separatorByte.toInt())
            },
        )
    }

    private fun buildMessage(
        serviceType: String,
        bodyWriter: ByteArrayOutputStream.() -> Unit,
    ): ByteArray {
        val body = ByteArrayOutputStream().apply {
            writeAscii(serviceType)
            write(protocolSpec.separatorByte.toInt())
            bodyWriter()
            write(protocolSpec.etxByte.toInt())
        }.toByteArray()

        val totalLength = 1 + 4 + body.size + 1
        val data = ByteArrayOutputStream().apply {
            write(protocolSpec.stxByte.toInt())
            writeAscii(totalLength.toString().padStart(4, '0'))
            write(body)
        }.toByteArray()

        return data + byteArrayOf(calculateBcc(data).toByte())
    }

    private fun calculateBcc(data: ByteArray): Int {
        var bcc = 0
        for (index in 1 until data.size) {
            val value = data[index].toInt() and 0xff
            bcc = bcc xor value
            if (value == (protocolSpec.etxByte.toInt() and 0xff)) {
                break
            }
        }
        return bcc or (protocolSpec.blankByte.toInt() and 0xff)
    }

    private fun ByteArrayOutputStream.writeAscii(value: String) {
        write(value.toByteArray(Charsets.US_ASCII))
    }

    companion object {
        private const val CARDLESS_CANCEL_TAG = "02"
        private const val CARDLESS_CANCEL_VALUE_LENGTH = "026"
        private const val CARDLESS_CANCEL_CODE = "CANC"
        private const val APPROVAL_NUMBER_LENGTH = 12
    }
}
