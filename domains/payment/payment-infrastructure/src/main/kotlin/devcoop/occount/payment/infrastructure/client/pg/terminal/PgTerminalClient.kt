package devcoop.occount.payment.infrastructure.client.pg.terminal

import devcoop.occount.payment.infrastructure.client.pg.config.PgProperties
import devcoop.occount.payment.infrastructure.client.pg.model.PgProduct
import devcoop.occount.payment.infrastructure.client.pg.model.PgResponse
import devcoop.occount.payment.infrastructure.client.pg.protocol.PgMessageBuilder
import devcoop.occount.payment.infrastructure.client.pg.protocol.PgMessageParser
import devcoop.occount.payment.infrastructure.client.pg.transport.PgTransport
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.SocketTimeoutException
import java.util.concurrent.locks.ReentrantLock

@Component
class PgTerminalClient(
    private val transport: PgTransport,
    private val properties: PgProperties,
) {
    private val log = LoggerFactory.getLogger(PgTerminalClient::class.java)
    private val transactionLock = ReentrantLock()

    fun approve(amount: Int, items: List<PgProduct>): PgResponse {
        return execute(
            request = PgMessageBuilder.buildPaymentMessage(amount, items),
            timeoutResult = PgResponse(
                success = false,
                message = "거래가 시간 초과로 취소되었습니다",
                errorCode = "TRANSACTION_TIMEOUT",
                transaction = null,
                card = null,
                additional = null,
                rawResponse = "timeout",
            ),
        )
    }

    fun cancel(
        amount: Int,
        approvalDate: String,
        approvalNumber: String,
        items: List<PgProduct> = emptyList(),
    ): PgResponse {
        return execute(
            request = PgMessageBuilder.buildCancelMessage(
                amount = amount,
                approvalDate = approvalDate,
                approvalNumber = approvalNumber,
                products = items,
            ),
            timeoutResult = PgResponse(
                success = false,
                message = "처리 시간 초과",
                errorCode = "TIMEOUT",
                transaction = null,
                card = null,
                additional = null,
                rawResponse = "timeout",
            ),
        )
    }

    private fun execute(request: ByteArray, timeoutResult: PgResponse): PgResponse {
        if (!transactionLock.tryLock()) {
            return PgResponse(
                success = false,
                message = "이미 진행 중인 거래가 있습니다",
                errorCode = "TRANSACTION_IN_PROGRESS",
                transaction = null,
                card = null,
                additional = null,
                rawResponse = null,
            )
        }

        return try {
            transport.ensureConnected()
            logMessage("send", request)
            transport.send(request)
            awaitResponse(timeoutResult)
        } catch (ex: Exception) {
            transport.close()
            throw ex
        } finally {
            transactionLock.unlock()
        }
    }

    private fun awaitResponse(timeoutResult: PgResponse): PgResponse {
        val startTime = System.currentTimeMillis()
        var lastStxResponse: ByteArray? = null

        while (System.currentTimeMillis() - startTime < properties.transactionTimeoutMillis) {
            val response = try {
                transport.receive()
            } catch (_: SocketTimeoutException) {
                continue
            }

            logMessage("receive", response)

            when {
                response.contentEquals(FORM_FEED_RESPONSE) -> {
                    sendAck()
                }

                response.isStxResponse() -> {
                    if (PgMessageParser.isIntermediateStxResponse(response)) {
                        sendAck()
                        continue
                    }

                    if (PgMessageParser.parsePaymentResponse(response) != null) {
                        lastStxResponse = response
                    }
                    sendAck()
                }

                response.contentEquals(DLE_RESPONSE) -> {
                    return lastStxResponse
                        ?.let(PgMessageParser::parsePaymentResponse)
                        ?: PgResponse(
                            success = false,
                            message = "사용자가 결제를 취소했습니다.",
                            errorCode = "USER_CANCELLED",
                            transaction = null,
                            card = null,
                            additional = null,
                            rawResponse = response.toHexString(),
                        )
                }

                else -> {
                    val parsed = PgMessageParser.parsePaymentResponse(response)
                    if (parsed != null) {
                        return parsed
                    }
                }
            }
        }

        return timeoutResult
    }

    private fun sendAck() {
        transport.send(ACK_RESPONSE)
        logMessage("ack", ACK_RESPONSE)
    }

    private fun logMessage(prefix: String, data: ByteArray) {
        log.info(
            "PG {} - hex: {}, length: {}",
            prefix,
            data.toHexString(),
            data.size,
        )
    }

        private fun ByteArray.isStxResponse(): Boolean = isNotEmpty() && this[0] == STX

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

    private companion object {
        const val STX: Byte = 0x02
        val ACK_RESPONSE: ByteArray = byteArrayOf(0x06, 0x06, 0x06)
        val DLE_RESPONSE: ByteArray = byteArrayOf(0x10)
        val FORM_FEED_RESPONSE: ByteArray = byteArrayOf(0x0c, 0x0c, 0x0c)
    }
}
