package devcoop.occount.order.application.usecase.order.get

import devcoop.occount.order.application.exception.OrderNotFoundException
import devcoop.occount.order.application.output.OrderRepository
import devcoop.occount.order.application.shared.OrderResponse
import devcoop.occount.order.application.support.OrderResponseMapper
import org.springframework.stereotype.Service

@Service
class GetOrderUseCase(
    private val orderRepository: OrderRepository,
    private val orderResponseMapper: OrderResponseMapper,
) {
    fun getOrder(orderId: String): OrderResponse {
        val order = orderRepository.findById(orderId) ?: throw OrderNotFoundException()
        return orderResponseMapper.toResponse(order)
    }
}
