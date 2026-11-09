package devcoop.occount.payment.infrastructure.event

import devcoop.occount.core.common.event.DomainEventHeaders
import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.OrderPaymentCancellationRequestedEvent
import devcoop.occount.core.common.event.OrderPaymentCompensationRequestedEvent
import devcoop.occount.core.common.event.OrderPaymentRequestedEvent
import devcoop.occount.db.outbox.ConsumedEventRepository
import devcoop.occount.db.outbox.IdempotencyTracker
import devcoop.occount.payment.application.exception.DuplicateEventException
import devcoop.occount.payment.application.usecase.payment.CancelPendingOrderPaymentUseCase
import devcoop.occount.payment.application.usecase.payment.CompensateOrderPaymentUseCase
import devcoop.occount.payment.application.usecase.payment.ExecuteVanPaymentUseCase
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

@Component
class OrderPaymentEventListener(
    private val objectMapper: ObjectMapper,
    private val executeVanPaymentUseCase: ExecuteVanPaymentUseCase,
    private val cancelPendingOrderPaymentUseCase: CancelPendingOrderPaymentUseCase,
    private val compensateOrderPaymentUseCase: CompensateOrderPaymentUseCase,
    consumedEventRepository: ConsumedEventRepository,
) {
    private val idempotency = IdempotencyTracker(CONSUMER_NAME, consumedEventRepository)
    private val cancelIdempotency = IdempotencyTracker(CANCEL_CONSUMER_NAME, consumedEventRepository)

    @KafkaListener(
        topics = [DomainTopics.PAYMENT_COMMANDS],
        groupId = CONSUMER_NAME,
        // payment.command.v1 은 kioskId 파티셔닝(파티션 6개). 단말별 병렬 처리를 위해 파티션 수만큼 스레드를 둔다.
        concurrency = "6",
    )
    fun handlePaymentCommand(
        payload: String,
        @Header(DomainEventHeaders.EVENT_ID) eventId: String,
        @Header(DomainEventHeaders.EVENT_TYPE) eventType: String,
    ) {
        when (eventType) {
            DomainEventTypes.ORDER_PAYMENT_REQUESTED -> handlePaymentRequested(payload, eventId)
            DomainEventTypes.ORDER_PAYMENT_CANCELLATION_REQUESTED -> handlePaymentCancellation(payload, eventId)
            DomainEventTypes.ORDER_PAYMENT_COMPENSATION_REQUESTED -> handlePaymentCompensation(payload, eventId)
            else -> {
                log.warn("알 수 없는 결제 커맨드 eventType={} eventId={}", eventType, eventId)
                throw IllegalStateException("Unknown payment command eventType=$eventType")
            }
        }
    }

    /**
     * 결제 취소 커맨드는 결제 커맨드와 분리된 전용 토픽/리스너(독립 컨슈머 스레드)에서 처리한다.
     * 진행 중 VAN 결제가 payment.command.v1 컨슈머 스레드를 최대 30초 블로킹해도, 이 리스너 스레드가
     * 즉시 단말 승인 취소(requestPendingApprovalCancellation)를 호출해 카드 대기 상태를 중단시킨다.
     */
    @KafkaListener(
        topics = [DomainTopics.PAYMENT_CANCEL_COMMANDS],
        groupId = CANCEL_CONSUMER_NAME,
        concurrency = "3",
    )
    fun handlePaymentCancelCommand(
        payload: String,
        @Header(DomainEventHeaders.EVENT_ID) eventId: String,
        @Header(DomainEventHeaders.EVENT_TYPE) eventType: String,
    ) {
        if (eventType != DomainEventTypes.ORDER_PAYMENT_CANCELLATION_REQUESTED) {
            log.warn("결제 취소 토픽에 예상치 못한 eventType={} eventId={}", eventType, eventId)
            throw IllegalStateException("Unexpected cancel command eventType=$eventType")
        }
        val event = objectMapper.readValue<OrderPaymentCancellationRequestedEvent>(payload)
        log.info("결제 취소 요청 이벤트 수신(전용 리스너) - orderId={} eventId={}", event.orderId, eventId)
        cancelPendingOrderPaymentUseCase.cancel(event) {
            cancelIdempotency.recordOrThrowOnDuplicate(eventId) { throw DuplicateEventException() }
        }
    }

    private fun handlePaymentRequested(payload: String, eventId: String) {
        val event = objectMapper.readValue<OrderPaymentRequestedEvent>(payload)
        log.info("결제 요청 이벤트 수신 - orderId={} eventId={}", event.orderId, eventId)
        executeVanPaymentUseCase.execute(
            event = event,
            recordConsumption = { recordConsumption(eventId) },
        )
    }

    private fun handlePaymentCancellation(payload: String, eventId: String) {
        val event = objectMapper.readValue<OrderPaymentCancellationRequestedEvent>(payload)
        log.info("결제 취소 요청 이벤트 수신 - orderId={} eventId={}", event.orderId, eventId)
        cancelPendingOrderPaymentUseCase.cancel(event) { recordConsumption(eventId) }
    }

    private fun handlePaymentCompensation(payload: String, eventId: String) {
        val event = objectMapper.readValue<OrderPaymentCompensationRequestedEvent>(payload)
        log.info("결제 보상 요청 이벤트 수신 - orderId={} eventId={}", event.orderId, eventId)
        compensateOrderPaymentUseCase.compensate(event) { recordConsumption(eventId) }
    }

    private fun recordConsumption(eventId: String) {
        idempotency.recordOrThrowOnDuplicate(eventId) { throw DuplicateEventException() }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OrderPaymentEventListener::class.java)
        private const val CONSUMER_NAME = "payment-command-v1"
        private const val CANCEL_CONSUMER_NAME = "payment-command-cancel-v1"
    }
}
