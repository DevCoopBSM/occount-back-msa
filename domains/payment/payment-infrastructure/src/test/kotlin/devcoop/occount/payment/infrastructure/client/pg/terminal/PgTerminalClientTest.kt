package devcoop.occount.payment.infrastructure.client.pg.terminal

import devcoop.occount.payment.infrastructure.client.pg.config.PgProperties
import devcoop.occount.payment.infrastructure.client.pg.model.PgProduct
import devcoop.occount.payment.infrastructure.client.pg.transport.PgTransport
import java.net.SocketTimeoutException
import java.nio.charset.Charset
import kotlin.test.Test
import kotlin.test.assertEquals

class PgTerminalClientTest {
    @Test
    fun `approve returns parsed result after stx and dle responses`() {
        val transport = FakePgTransport(
            responses = ArrayDeque(
                listOf(
                    buildResponse(
                        "0103MSG0001",
                        "D1",
                        "1234********1234",
                        "2000",
                        "00",
                        "",
                        "TERM01",
                        "MERCHANT01",
                        "20260403",
                        "101010",
                        "TX-1",
                        "ACQ01",
                        "Acquirer",
                        "Issuer VISA",
                        "ISS01",
                        "2",
                        "정상승인\u001eAPPROVAL1",
                        "IC",
                        "UUID-1",
                    ),
                    byteArrayOf(0x10),
                ),
            ),
        )
        val client = PgTerminalClient(
            transport = transport,
            properties = PgProperties(
                host = "127.0.0.1",
                port = 5555,
                transactionTimeoutMillis = 100,
            ),
        )

        val result = client.approve(
            amount = 2000,
            items = listOf(PgProduct(name = "Americano", quantity = 1, total = 2000)),
        )

        assertEquals(true, result.success)
        assertEquals("APPROVAL1", result.transaction?.approvalNumber)
        assertEquals(2, transport.sentMessages.size)
    }

    @Test
    fun `approve returns timeout result when responses do not arrive`() {
        val transport = FakePgTransport(throwTimeout = true)
        val client = PgTerminalClient(
            transport = transport,
            properties = PgProperties(
                host = "127.0.0.1",
                port = 5555,
                readTimeoutMillis = 1,
                transactionTimeoutMillis = 10,
            ),
        )

        val result = client.approve(amount = 2000, items = emptyList())

        assertEquals(false, result.success)
        assertEquals("TRANSACTION_TIMEOUT", result.errorCode)
    }

    private class FakePgTransport(
        val responses: ArrayDeque<ByteArray> = ArrayDeque(),
        private val throwTimeout: Boolean = false,
    ) : PgTransport {
        val sentMessages = mutableListOf<ByteArray>()

        override fun ensureConnected() = Unit

        override fun send(data: ByteArray) {
            sentMessages += data
        }

        override fun receive(): ByteArray {
            if (throwTimeout) {
                throw SocketTimeoutException("timeout")
            }
            return responses.removeFirst()
        }

        override fun close() = Unit
    }

    private fun buildResponse(vararg fields: String): ByteArray {
        return ("\u0002" + fields.joinToString("\u001c") + "\u0003").toByteArray(Charset.forName("EUC-KR"))
    }
}
