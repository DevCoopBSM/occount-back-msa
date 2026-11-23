package devcoop.occount.order.application.query

import devcoop.occount.order.application.exception.OrderNotFoundException
import devcoop.occount.order.application.output.OrderRepository
import devcoop.occount.order.application.shared.OrderHistoryListResponse
import devcoop.occount.order.application.shared.OrderResponse
import devcoop.occount.order.application.shared.OrderStreamEvent
import devcoop.occount.order.application.support.OrderAccessValidator
import devcoop.occount.order.application.support.OrderHistoryMapper
import devcoop.occount.order.application.support.OrderResponseMapper
import devcoop.occount.order.application.support.OrderStreamEventMapper
import devcoop.occount.order.domain.order.OrderAggregate
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
    fun getOrder(orderId: Long, userId: Long?, guestOrderToken: String?): OrderResponse {
        val order = findAccessibleOrder(orderId, userId, guestOrderToken)
        return orderResponseMapper.toResponse(order)
    }

    @Transactional(readOnly = true)
    fun getMyOrders(userId: Long, pageable: Pageable): OrderHistoryListResponse {
        val page = orderRepository.findByUserId(userId, pageable)
            .map(orderHistoryMapper::toHistoryItem)
        return OrderHistoryListResponse.from(page)
    }

    fun validateOrderAccess(orderId: Long, userId: Long?, guestOrderToken: String?) {
        findAccessibleOrder(orderId, userId, guestOrderToken)
    }

    fun getOrderStreamEvent(orderId: Long, userId: Long?, guestOrderToken: String?): OrderStreamEvent {
        val order = findAccessibleOrder(orderId, userId, guestOrderToken)
        return orderStreamEventMapper.toStreamEvent(order)
    }

    private fun findAccessibleOrder(orderId: Long, userId: Long?, guestOrderToken: String?): OrderAggregate {
        val order = orderRepository.findById(orderId) ?: throw OrderNotFoundException()
        OrderAccessValidator.validate(order, userId, guestOrderToken)
        return order
    }
}
