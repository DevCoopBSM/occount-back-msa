package devcoop.occount.order.api.order

import devcoop.occount.core.common.auth.RequestAuthPrincipalResolver
import devcoop.occount.order.application.order.OrderService
import devcoop.occount.order.application.order.OrderRequest
import devcoop.occount.order.application.order.OrderResponse
import devcoop.occount.order.domain.order.OrderStatus
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/orders")
class OrderController(
    private val orderService: OrderService,
) {
    @PostMapping
    fun handleOrder(
        @RequestBody orderRequest: OrderRequest,
        httpServletRequest: HttpServletRequest,
    ): ResponseEntity<OrderResponse> {
        val authPrincipal = RequestAuthPrincipalResolver.resolve(httpServletRequest)
        val response = orderService.order(orderRequest, authPrincipal.userId)
        return ResponseEntity.status(resolveStatus(response)).body(response)
    }

    @PostMapping("/{orderId}/cancel")
    fun cancelOrder(
        @PathVariable orderId: String,
        httpServletRequest: HttpServletRequest,
    ): ResponseEntity<OrderResponse> {
        val authPrincipal = RequestAuthPrincipalResolver.resolve(httpServletRequest)
        val response = orderService.cancel(orderId, authPrincipal.userId)
        return ResponseEntity.status(HttpStatus.OK).body(response)
    }

    private fun resolveStatus(response: OrderResponse): HttpStatus {
        return when (response.status) {
            OrderStatus.COMPLETED -> HttpStatus.OK
            OrderStatus.TIMED_OUT -> HttpStatus.GATEWAY_TIMEOUT
            OrderStatus.FAILED,
            OrderStatus.CANCELLED,
            OrderStatus.COMPENSATION_FAILED,
            -> HttpStatus.CONFLICT

            else -> HttpStatus.OK
        }
    }
}
