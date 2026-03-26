package devcoop.occount.order.application.order

import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class OrderService(
    private val eventPublisher: EventPublisher,
) {
    private companion object {
        const val ORDER_REQUESTED_EVENT_TYPE = "OrderRequestedEvent"
    }

    @Transactional
    fun order(request: OrderRequest, userId: Long) {
        eventPublisher.publish(
            topic = DomainTopics.ORDER_REQUESTED,
            key = UUID.randomUUID().toString(),
            eventType = ORDER_REQUESTED_EVENT_TYPE,
            payload = mapOf(
                "orderInfos" to request.orderInfos.map { orderInfo ->
                    "userId" to userId
                    mapOf(
                        "itemId" to orderInfo.itemId,
                        "quantity" to orderInfo.orderQuantity,
                    )
                },
            ),
        )
    }
}
