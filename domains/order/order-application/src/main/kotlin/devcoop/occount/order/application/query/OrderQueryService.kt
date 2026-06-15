package devcoop.occount.order.application.query

import devcoop.occount.order.application.exception.OrderNotFoundException
import devcoop.occount.order.application.output.OrderRepository
import devcoop.occount.order.application.shared.OrderHistoryListResponse
import devcoop.occount.order.application.shared.OrderResponse
import devcoop.occount.order.application.shared.OrderStreamEvent
import devcoop.occount.order.application.support.OrderHistoryMapper
import devcoop.occount.order.application.support.OrderResponseMapper
import devcoop.occount.order.application.support.OrderStreamEventMapper
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderQueryService(
    private val orderRepository: OrderRepository,
    private val orderResponseMapper: OrderResponseMapper,
    private val orderStreamEventMapper: OrderStreamEventMapper,
    private val orderHistoryMapper: OrderHistoryMapper,
) {
    fun getOrder(orderId: Long): OrderResponse {
        val order = orderRepository.findById(orderId) ?: throw OrderNotFoundException()
        return orderResponseMapper.toResponse(order)
    }

    @Transactional(readOnly = true)
    fun getMyOrders(userId: Long, pageable: Pageable): OrderHistoryListResponse {
        val page = orderRepository.findByUserId(userId, pageable)
            .map(orderHistoryMapper::toHistoryItem)
        return OrderHistoryListResponse.from(page)
    }

    fun getOrderStreamEvent(orderId: Long): OrderStreamEvent {
        val order = orderRepository.findById(orderId) ?: throw OrderNotFoundException()
        return orderStreamEventMapper.toStreamEvent(order)
    }
}
