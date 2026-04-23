package devcoop.occount.payment.infrastructure.client.van

import devcoop.occount.payment.application.dto.request.ItemCommand
import devcoop.occount.payment.application.dto.response.VanResult
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class VanTerminalClient(
    terminal: VanProperties.Terminal,
    private val messageBuilder: VanMessageBuilder,
    private val messageParser: VanMessageParser,
    private val protocolSpec: VanProtocolSpec,
) {
    private val log = LoggerFactory.getLogger(VanTerminalClient::class.java)
    private val socketConnection = VanSocketConnection(terminal.host, terminal.port)
    private val transactionInProgress = AtomicBoolean(false)
    private val cancellationRequested = AtomicBoolean(false)
    private val currentTransactionType = AtomicReference(TransactionType.NONE)
    private val currentPaymentKey = AtomicReference<String?>(null)
    private val approvalPhase = AtomicReference(ApprovalPhase.IDLE)

    fun approve(amount: Int, items: List<ItemCommand>, paymentKey: String? = null): VanResult {
        return executeTransaction(
            actionName = "카드결제",
            transactionType = TransactionType.APPROVE,
            requestMessage = messageBuilder.buildPaymentMessage(amount, items),
            paymentKey = paymentKey,
        )
    }

    fun refund(approvalNumber: String, approvalDate: String, amount: Int): VanResult {
        return executeTransaction(
            actionName = "카드환불",
            transactionType = TransactionType.REFUND,
            requestMessage = messageBuilder.buildRefundMessage(amount, approvalDate, approvalNumber),
        )
    }

    fun requestPendingApprovalCancellation(paymentKey: String) {
        if (!transactionInProgress.get() || currentTransactionType.get() != TransactionType.APPROVE) {
            log.info("결제 대기 취소 요청 무시 - 진행 중인 승인 거래 없음 orderId={}", paymentKey)
            return
        }

        if (currentPaymentKey.get() != paymentKey) {
            log.info(
                "결제 대기 취소 요청 무시 - 다른 주문의 승인 거래 처리 중 orderId={} currentOrderId={}",
                paymentKey,
                currentPaymentKey.get(),
            )
            return
        }

        val phase = approvalPhase.get()
        if (phase != ApprovalPhase.REQUESTED && phase != ApprovalPhase.WAITING_FOR_CARD) {
            log.info("결제 대기 취소 요청 무시 - 이미 카드 처리 단계 진입 orderId={} phase={}", paymentKey, phase)
            return
        }

        log.info("카드 삽입 전 결제 취소 요청 수신 - orderId={}", paymentKey)
        cancellationRequested.set(true)
        sendTerminalCloseRequest(paymentKey)
    }

    private fun executeTransaction(
        actionName: String,
        transactionType: TransactionType,
        requestMessage: ByteArray,
        paymentKey: String? = null,
    ): VanResult {
        if (!transactionInProgress.compareAndSet(false, true)) {
            return VanResult(
                success = false,
                message = "이미 진행 중인 거래가 있습니다",
                errorCode = "TRANSACTION_IN_PROGRESS",
                transaction = null,
                card = null,
                additional = null,
                rawResponse = null,
            )
        }

        currentTransactionType.set(transactionType)
        currentPaymentKey.set(paymentKey)
        cancellationRequested.set(false)
        approvalPhase.set(if (transactionType == TransactionType.APPROVE) ApprovalPhase.REQUESTED else ApprovalPhase.NOT_CANCELLABLE)

        return try {
            doExecuteTransaction(actionName, requestMessage)
        } finally {
            transactionInProgress.set(false)
            cancellationRequested.set(false)
            currentTransactionType.set(TransactionType.NONE)
            currentPaymentKey.set(null)
            approvalPhase.set(ApprovalPhase.IDLE)
        }
    }

    private fun doExecuteTransaction(actionName: String, requestMessage: ByteArray): VanResult {
        val connected = if (currentTransactionType.get() == TransactionType.APPROVE) {
            socketConnection.refreshConnection()
        } else {
            socketConnection.ensureConnected()
        }

        if (!connected) {
            return connectionFailedResult()
        }

        return try {
            socketConnection.logMessage("발신", requestMessage)
            socketConnection.send(requestMessage)
            waitForResponse(actionName, requestMessage)
        } catch (e: IOException) {
            log.error("{} 요청 처리 중 소켓 오류 발생: {}", actionName, e.message, e)
            connectionFailedResult()
        }
    }

    private fun waitForResponse(actionName: String, requestMessage: ByteArray): VanResult {
        val deadline = System.nanoTime() + protocolSpec.transactionTimeoutNanos
        var lastStxResponse: ByteArray? = null
        var approvalCandidate: VanResult? = null

        while (System.nanoTime() < deadline) {
            if (cancellationRequested.get()) {
                approvalCandidate?.let { return finalizeApprovedOnStx(it) }
                return userCancelledResult()
            }

            val response = try {
                socketConnection.receive()
            } catch (e: IOException) {
                if (cancellationRequested.get()) {
                    approvalCandidate?.let { return finalizeApprovedOnStx(it) }
                    return userCancelledResult()
                }
                log.error("{} 응답 수신 중 오류 발생: {}", actionName, e.message, e)
                if (!socketConnection.reconnect()) {
                    continue
                }
                if (approvalCandidate == null) {
                    log.info("재연결 후 요청 재전송 - actionName={}", actionName)
                    socketConnection.logMessage("발신", requestMessage)
                    socketConnection.send(requestMessage)
                }
                continue
            }

            if (response == null) {
                if (cancellationRequested.get()) {
                    approvalCandidate?.let { return finalizeApprovedOnStx(it) }
                    return userCancelledResult()
                }
                continue
            }

            for (frame in protocolSpec.splitFrames(response)) {
                socketConnection.logMessage("수신", frame)
                val responseHex = protocolSpec.toHex(frame)

                when {
                    frame.firstOrNull() == protocolSpec.stxByte -> {
                        val parsed = messageParser.parsePaymentResponse(frame)
                        sendAck()

                        if (parsed == null) {
                            if (currentTransactionType.get() == TransactionType.APPROVE) {
                                approvalPhase.set(ApprovalPhase.WAITING_FOR_CARD)
                            }
                            continue
                        }

                        if (currentTransactionType.get() == TransactionType.APPROVE) {
                            approvalPhase.set(ApprovalPhase.CARD_PROCESSING)
                        }
                        if (isApprovalCandidate(parsed)) {
                            approvalCandidate = parsed
                        }
                        lastStxResponse = frame
                    }

                    responseHex == protocolSpec.dleHex || responseHex == protocolSpec.dleCompletedHex -> {
                        approvalCandidate?.let { return finalizeApprovedOnStx(it) }

                        if (cancellationRequested.get() || lastStxResponse == null) {
                            return userCancelledResult()
                        }

                        if (currentTransactionType.get() == TransactionType.APPROVE) {
                            approvalPhase.set(ApprovalPhase.CARD_PROCESSING)
                        }
                        val parsed = messageParser.parsePaymentResponse(lastStxResponse)
                        if (parsed != null) {
                            return parsed
                        }
                        log.error("DLE 수신 후 응답 파싱 실패 - 비정상 응답")
                        return abnormalResponseResult()
                    }

                    responseHex == protocolSpec.formFeedHex -> {
                        sendAck()
                    }

                    else -> {
                        val parsed = messageParser.parsePaymentResponse(frame)
                        if (parsed != null) {
                            if (currentTransactionType.get() == TransactionType.APPROVE) {
                                approvalPhase.set(ApprovalPhase.CARD_PROCESSING)
                            }
                            return parsed
                        }
                    }
                }
            }
        }

        approvalCandidate?.let { return finalizeApprovedOnStx(it) }
        return timeoutResult()
    }

    private fun isApprovalCandidate(result: VanResult): Boolean {
        return result.success &&
            result.additional?.approvalStatus == APPROVED_STATUS &&
            !result.transaction?.approvalNumber.isNullOrBlank()
    }

    private fun finalizeApprovedOnStx(result: VanResult): VanResult {
        log.info(
            "최종 성공 STX 기준 결제 승인 확정 - approvalNumber={} transactionId={}",
            result.transaction?.approvalNumber,
            result.transaction?.transactionId,
        )
        return result
    }

    private fun sendTerminalCloseRequest(paymentKey: String): Boolean {
        return runCatching {
            val closeMessage = messageBuilder.buildTerminalCloseMessage()
            socketConnection.logMessage("발신", closeMessage)
            socketConnection.send(closeMessage)
            log.info("단말기 화면 종료 전문 전송 - orderId={}", paymentKey)
            true
        }.getOrElse { e ->
            log.error("단말기 화면 종료 전문 전송 실패 - orderId={} message={}", paymentKey, e.message, e)
            false
        }
    }

    private fun sendAck() {
        runCatching {
            socketConnection.send(protocolSpec.ackBytes)
        }.onFailure { e ->
            log.error("ACK 전송 실패: {}", e.message, e)
        }
    }

    private fun timeoutResult(): VanResult {
        return VanResult(
            success = false,
            message = "거래가 시간 초과로 취소되었습니다",
            errorCode = "TRANSACTION_TIMEOUT",
            transaction = null,
            card = null,
            additional = null,
            rawResponse = "timeout",
        )
    }

    private fun userCancelledResult(): VanResult {
        return VanResult(
            success = false,
            message = "사용자가 결제를 취소했습니다.",
            errorCode = "USER_CANCELLED",
            transaction = null,
            card = null,
            additional = null,
            rawResponse = null,
        )
    }

    private fun connectionFailedResult(): VanResult {
        return VanResult(
            success = false,
            message = "연결 실패",
            errorCode = "CONNECTION_FAILED",
            transaction = null,
            card = null,
            additional = null,
            rawResponse = null,
        )
    }

    private fun abnormalResponseResult(): VanResult {
        return VanResult(
            success = false,
            message = "비정상적인 단말기 응답",
            errorCode = "ABNORMAL_RESPONSE",
            transaction = null,
            card = null,
            additional = null,
            rawResponse = null,
        )
    }

    fun close() {
        socketConnection.close()
    }

    private enum class TransactionType {
        NONE,
        APPROVE,
        REFUND,
    }

    private enum class ApprovalPhase {
        IDLE,
        REQUESTED,
        WAITING_FOR_CARD,
        CARD_PROCESSING,
        NOT_CANCELLABLE,
    }

    companion object {
        private const val APPROVED_STATUS = "APPROVED"
    }
}
