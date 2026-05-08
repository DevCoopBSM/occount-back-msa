package devcoop.occount.payment.infrastructure.event

import devcoop.occount.core.common.event.DomainEventHeaders
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.OrderPaymentCancellationRequestedEvent
import devcoop.occount.core.common.event.OrderPaymentCompensationRequestedEvent
import devcoop.occount.core.common.event.OrderPaymentRequestedEvent
import devcoop.occount.db.outbox.ConsumedEventJpaEntity
import devcoop.occount.db.outbox.ConsumedEventRepository
import devcoop.occount.payment.application.exception.DuplicateEventException
import devcoop.occount.payment.application.usecase.payment.CancelPendingOrderPaymentUseCase
import devcoop.occount.payment.application.usecase.payment.CompensateOrderPaymentUseCase
import devcoop.occount.payment.application.usecase.payment.ExecuteVanPaymentUseCase
import org.springframework.dao.DataIntegrityViolationException
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
    private val consumedEventRepository: ConsumedEventRepository,
) {
    @KafkaListener(
        topics = [DomainTopics.ORDER_PAYMENT_REQUESTED],
        groupId = PAYMENT_REQUESTED_CONSUMER,
    )
    fun executeVanPayment(
        payload: String,
        @Header(DomainEventHeaders.EVENT_ID) eventId: String,
    ) {
        val event = objectMapper.readValue<OrderPaymentRequestedEvent>(payload)
        log.info("결제 요청 이벤트 수신 - orderId={} eventId={}", event.orderId, eventId)
        executeVanPaymentUseCase.execute(
            event = event,
            recordConsumption = { saveConsumedEvent(PAYMENT_REQUESTED_CONSUMER, eventId) },
        )
    }

    @KafkaListener(
        topics = [DomainTopics.ORDER_PAYMENT_CANCELLATION_REQUESTED],
        groupId = PAYMENT_CANCELLATION_REQUESTED_CONSUMER,
    )
    fun cancelPendingPayment(
        payload: String,
        @Header(DomainEventHeaders.EVENT_ID) eventId: String,
    ) {
        if (isProcessed(PAYMENT_CANCELLATION_REQUESTED_CONSUMER, eventId)) {
            log.info("결제 취소 요청 이벤트 중복 스킵 - eventId={}", eventId)
            return
        }

        val event = objectMapper.readValue<OrderPaymentCancellationRequestedEvent>(payload)
        log.info("결제 취소 요청 이벤트 수신 - orderId={} eventId={}", event.orderId, eventId)
        cancelPendingOrderPaymentUseCase.cancel(event)
        saveConsumedEvent(PAYMENT_CANCELLATION_REQUESTED_CONSUMER, eventId)
    }

    @KafkaListener(
        topics = [DomainTopics.ORDER_PAYMENT_COMPENSATION_REQUESTED],
        groupId = PAYMENT_COMPENSATION_REQUESTED_CONSUMER,
    )
    fun compensatePayment(
        payload: String,
        @Header(DomainEventHeaders.EVENT_ID) eventId: String,
    ) {
        if (isProcessed(PAYMENT_COMPENSATION_REQUESTED_CONSUMER, eventId)) {
            log.info("결제 보상 요청 이벤트 중복 스킵 - eventId={}", eventId)
            return
        }

        val event = objectMapper.readValue<OrderPaymentCompensationRequestedEvent>(payload)
        log.info("결제 보상 요청 이벤트 수신 - orderId={} eventId={}", event.orderId, eventId)
        compensateOrderPaymentUseCase.compensate(event)
        saveConsumedEvent(PAYMENT_COMPENSATION_REQUESTED_CONSUMER, eventId)
    }

    private fun isProcessed(consumerName: String, eventId: String): Boolean {
        return consumedEventRepository.existsById(processedEventId(consumerName, eventId))
    }

    private fun saveConsumedEvent(consumerName: String, eventId: String) {
        try {
            consumedEventRepository.save(
                ConsumedEventJpaEntity(
                    id = processedEventId(consumerName, eventId),
                    consumerName = consumerName,
                    eventId = eventId,
                ),
            )
        } catch (_: DataIntegrityViolationException) {
            throw DuplicateEventException()
        }
    }

    private fun processedEventId(consumerName: String, eventId: String): String {
        return "$consumerName:$eventId"
    }

    companion object {
        private val log = LoggerFactory.getLogger(OrderPaymentEventListener::class.java)
        private const val PAYMENT_REQUESTED_CONSUMER = "order-payment-requested-v1"
        private const val PAYMENT_CANCELLATION_REQUESTED_CONSUMER = "order-payment-cancellation-requested-v1"
        private const val PAYMENT_COMPENSATION_REQUESTED_CONSUMER = "order-payment-compensation-requested-v1"
    }
}
