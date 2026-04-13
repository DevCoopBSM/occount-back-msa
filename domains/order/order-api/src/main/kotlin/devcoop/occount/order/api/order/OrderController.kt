package devcoop.occount.order.api.order

import devcoop.occount.core.common.auth.RequestAuthPrincipalResolver
import devcoop.occount.order.application.shared.OrderRequest
import devcoop.occount.order.application.shared.OrderResponse
import devcoop.occount.order.application.usecase.order.cancel.CancelOrderUseCase
import devcoop.occount.order.application.usecase.order.create.CreateOrderUseCase
import devcoop.occount.order.domain.order.OrderStatus
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.async.DeferredResult
import kotlin.math.max

@RestController
@RequestMapping("/orders")
class OrderController(
    private val createOrderUseCase: CreateOrderUseCase,
    private val cancelOrderUseCase: CancelOrderUseCase,
    @param:Value("\${order.timeout-seconds:30}") private val timeoutSeconds: Long,
    @param:Value("\${order.async-timeout-buffer-millis:10000}") private val asyncTimeoutBufferMillis: Long,
) {
    @PostMapping
    fun createOrder(
        @RequestBody orderRequest: OrderRequest,
        httpServletRequest: HttpServletRequest,
    ): DeferredResult<ResponseEntity<OrderResponse>> {
        val authPrincipal = RequestAuthPrincipalResolver.resolve(httpServletRequest)

        val deferredResult = DeferredResult<ResponseEntity<OrderResponse>>(
            timeoutSeconds * 1000 + max(asyncTimeoutBufferMillis, MIN_ASYNC_TIMEOUT_BUFFER_MILLIS),
        )

        createOrderUseCase.placeOrder(orderRequest, authPrincipal.userId)
            .whenComplete { response, throwable ->
                if (throwable != null) {
                    deferredResult.setErrorResult(unwrap(throwable))
                    return@whenComplete
                }

                deferredResult.setResult(
                    ResponseEntity.status(resolveStatus(response)).body(response),
                )
            }

        return deferredResult
    }

    @PostMapping("/{orderId}/cancel")
    fun cancelOrder(
        @PathVariable orderId: String,
        httpServletRequest: HttpServletRequest,
    ): ResponseEntity<OrderResponse> {
        val authPrincipal = RequestAuthPrincipalResolver.resolve(httpServletRequest)
        val response = cancelOrderUseCase.cancel(orderId, authPrincipal.userId)
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

    private fun unwrap(throwable: Throwable): Throwable {
        return throwable.cause?.takeIf {
            throwable is java.util.concurrent.CompletionException ||
                throwable is java.util.concurrent.ExecutionException
        } ?: throwable
    }

    companion object {
        private const val MIN_ASYNC_TIMEOUT_BUFFER_MILLIS = 5_000L
    }
}
