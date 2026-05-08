package devcoop.occount.order.application.query

import devcoop.occount.order.application.exception.OrderNotFoundException
import devcoop.occount.order.application.output.OrderRepository
import devcoop.occount.order.application.shared.OrderResponse
import devcoop.occount.order.application.shared.OrderStreamEvent
import devcoop.occount.order.application.support.OrderResponseMapper
import devcoop.occount.order.application.support.OrderStreamEventMapper
import org.springframework.stereotype.Service

@Service
class OrderQueryService(
    private val orderRepository: OrderRepository,
    private val orderResponseMapper: OrderResponseMapper,
    private val orderStreamEventMapper: OrderStreamEventMapper,
) {
    fun getOrder(orderId: Long): OrderResponse {
        val order = orderRepository.findById(orderId) ?: throw OrderNotFoundException()
        return orderResponseMapper.toResponse(order)
    }

    fun getOrderStreamEvent(orderId: Long): OrderStreamEvent {
        val order = orderRepository.findById(orderId) ?: throw OrderNotFoundException()
        return orderStreamEventMapper.toStreamEvent(order)
    }
}
