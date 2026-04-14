package devcoop.occount.order.api.order

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.order.application.exception.OrderInvalidTotalPriceException
import devcoop.occount.order.application.config.OrderTimeoutConfig
import devcoop.occount.order.application.shared.OrderRequest
import devcoop.occount.order.application.shared.OrderResponse
import devcoop.occount.order.application.usecase.order.cancel.CancelOrderUseCase
import devcoop.occount.order.application.usecase.order.create.CreateOrderUseCase
import devcoop.occount.order.domain.order.OrderStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import java.util.concurrent.CompletableFuture

class OrderControllerTest {
    private val createOrderUseCase = mock(CreateOrderUseCase::class.java)
    private val cancelOrderUseCase = mock(CancelOrderUseCase::class.java)
    private val controller = OrderController(
        createOrderUseCase,
        cancelOrderUseCase,
        OrderTimeoutConfig(timeoutSeconds = 30L, asyncTimeoutBufferMillis = 10_000L),
    )

    @Test
    fun `handle order returns resolved async response`() {
        val request = OrderRequest(
            items = emptyList(),
            paymentType = devcoop.occount.core.common.event.OrderPaymentType.PAYMENT,
            totalAmount = 0,
            kioskId = "kiosk-1",
        )
        val expected = OrderResponse(
            orderId = "order-1",
            status = OrderStatus.COMPLETED,
        )

        `when`(createOrderUseCase.placeOrder(request, 7L)).thenReturn(CompletableFuture.completedFuture(expected))

        val httpRequest = MockHttpServletRequest().apply {
            addHeader(AuthHeaders.AUTHENTICATED_USER_ID, "7")
        }

        val deferredResult = controller.createOrder(request, httpRequest)
        val response = deferredResult.result as org.springframework.http.ResponseEntity<*>

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(expected, response.body)
    }

    @Test
    fun `handle guest order with no user id header`() {
        val request = OrderRequest(
            items = emptyList(),
            paymentType = devcoop.occount.core.common.event.OrderPaymentType.CARD,
            totalAmount = 0,
            kioskId = "kiosk-1",
        )
        val expected = OrderResponse(
            orderId = "order-2",
            status = OrderStatus.COMPLETED,
        )

        `when`(createOrderUseCase.placeOrder(request, null)).thenReturn(CompletableFuture.completedFuture(expected))

        val httpRequest = MockHttpServletRequest()

        val deferredResult = controller.createOrder(request, httpRequest)
        val response = deferredResult.result as org.springframework.http.ResponseEntity<*>

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(expected, response.body)
    }

    @Test
    fun `handle order propagates business exception to async error handling`() {
        val request = OrderRequest(
            items = emptyList(),
            paymentType = devcoop.occount.core.common.event.OrderPaymentType.PAYMENT,
            totalAmount = 0,
            kioskId = "kiosk-1",
        )

        `when`(createOrderUseCase.placeOrder(request, 7L)).thenReturn(
            CompletableFuture.failedFuture(OrderInvalidTotalPriceException()),
        )

        val httpRequest = MockHttpServletRequest().apply {
            addHeader(AuthHeaders.AUTHENTICATED_USER_ID, "7")
        }

        val deferredResult = controller.createOrder(request, httpRequest)

        assertInstanceOf(OrderInvalidTotalPriceException::class.java, deferredResult.result)
    }
}
