package devcoop.occount.order.api.order

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.order.api.sse.OrderSseRegistry
import devcoop.occount.order.application.shared.OrderRequest
import devcoop.occount.order.application.shared.OrderResponse
import devcoop.occount.order.application.query.OrderQueryService
import devcoop.occount.order.application.usecase.order.cancel.CancelOrderUseCase
import devcoop.occount.order.application.usecase.order.create.CreateOrderUseCase
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/orders")
class OrderController(
    private val createOrderUseCase: CreateOrderUseCase,
    private val cancelOrderUseCase: CancelOrderUseCase,
    private val orderQueryService: OrderQueryService,
    private val orderSseRegistry: OrderSseRegistry,
) {
    @PostMapping
    fun createOrder(
        @RequestBody orderRequest: OrderRequest,
        @RequestHeader(value = AuthHeaders.KIOSK_ID, required = true) kioskId: String,
        @RequestHeader(value = AuthHeaders.AUTHENTICATED_USER_ID, required = false) userIdHeader: String?,
    ): ResponseEntity<OrderResponse> {
        val userId = userIdHeader?.toLongOrNull()
        val response = createOrderUseCase.placeOrder(orderRequest, userId, kioskId)
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response)
    }

    @GetMapping("/{orderId}")
    fun getOrder(
        @PathVariable orderId: Long,
    ): ResponseEntity<OrderResponse> {
        val response = orderQueryService.getOrder(orderId)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{orderId}/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamOrder(
        @PathVariable orderId: Long,
    ): SseEmitter {
        return orderSseRegistry.register(orderId) {
            orderQueryService.getOrderStreamEvent(orderId)
        }
    }

    @PostMapping("/{orderId}/cancel")
    fun cancelOrder(
        @PathVariable orderId: Long,
        @RequestHeader(value = AuthHeaders.KIOSK_ID, defaultValue = "1") kioskId: String,
    ): ResponseEntity<OrderResponse> {
        val response = cancelOrderUseCase.cancel(orderId, kioskId)
        return ResponseEntity.ok(response)
    }
}
