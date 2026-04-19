package devcoop.occount.payment.infrastructure.event

import devcoop.occount.core.common.event.DomainEventHeaders
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.OrderPaymentCancellationRequestedEvent
import devcoop.occount.core.common.event.OrderPaymentCompensationRequestedEvent
import devcoop.occount.core.common.event.OrderPaymentRequestedEvent
import devcoop.occount.db.outbox.ConsumedEventJpaEntity
import devcoop.occount.db.outbox.ConsumedEventRepository
import devcoop.occount.payment.application.usecase.payment.CancelPendingOrderPaymentUseCase
import devcoop.occount.payment.application.usecase.payment.CompensateOrderPaymentUseCase
import devcoop.occount.payment.application.usecase.payment.ExecuteVanPaymentUseCase
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.Instant

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
        if (isProcessed(PAYMENT_REQUESTED_CONSUMER, eventId)) {
            return
        }

        executeVanPaymentUseCase.execute(objectMapper.readValue<OrderPaymentRequestedEvent>(payload))
        markProcessed(PAYMENT_REQUESTED_CONSUMER, eventId)
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
            return
        }

        cancelPendingOrderPaymentUseCase.cancel(objectMapper.readValue<OrderPaymentCancellationRequestedEvent>(payload))
        markProcessed(PAYMENT_CANCELLATION_REQUESTED_CONSUMER, eventId)
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
            return
        }

        compensateOrderPaymentUseCase.compensate(objectMapper.readValue<OrderPaymentCompensationRequestedEvent>(payload))
        markProcessed(PAYMENT_COMPENSATION_REQUESTED_CONSUMER, eventId)
    }

    private fun isProcessed(consumerName: String, eventId: String): Boolean {
        return consumedEventRepository.existsById(processedEventId(consumerName, eventId))
    }

    private fun markProcessed(consumerName: String, eventId: String) {
        consumedEventRepository.save(
            ConsumedEventJpaEntity(
                id = processedEventId(consumerName, eventId),
                consumerName = consumerName,
                eventId = eventId,
                processedAt = Instant.now(),
            ),
        )
    }

    private fun processedEventId(consumerName: String, eventId: String): String {
        return "$consumerName:$eventId"
    }

    companion object {
        private const val PAYMENT_REQUESTED_CONSUMER = "order-payment-requested-v1"
        private const val PAYMENT_CANCELLATION_REQUESTED_CONSUMER = "order-payment-cancellation-requested-v1"
        private const val PAYMENT_COMPENSATION_REQUESTED_CONSUMER = "order-payment-compensation-requested-v1"
    }
}
