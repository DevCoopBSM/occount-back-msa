package devcoop.occount.order.api.order

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.order.api.sse.OrderSseRegistry
import devcoop.occount.order.application.exception.OrderTransactionFailedException
import devcoop.occount.order.application.shared.OrderItemRequest
import devcoop.occount.order.application.shared.OrderRequest
import devcoop.occount.order.application.shared.OrderResponse
import devcoop.occount.order.application.usecase.order.cancel.CancelOrderUseCase
import devcoop.occount.order.application.usecase.order.create.CreateOrderUseCase
import devcoop.occount.order.application.usecase.order.get.GetOrderUseCase
import devcoop.occount.order.domain.order.OrderStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.springframework.http.HttpStatus
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

class OrderControllerTest {
    private val createOrderUseCase = mock(CreateOrderUseCase::class.java)
    private val cancelOrderUseCase = mock(CancelOrderUseCase::class.java)
    private val getOrderUseCase = mock(GetOrderUseCase::class.java)
    private val orderSseRegistry = mock(OrderSseRegistry::class.java)
    private val controller = OrderController(
        createOrderUseCase,
        cancelOrderUseCase,
        getOrderUseCase,
        orderSseRegistry,
    )

    @Test
    fun `createOrder returns ACCEPTED with response body`() {
        val request = OrderRequest(
            items = listOf(OrderItemRequest(itemId = 1L, quantity = 1)),
        )
        val expected = OrderResponse(orderId = 1L, status = OrderStatus.PROCESSING)

        `when`(createOrderUseCase.placeOrder(request, 7L, "kiosk-1")).thenReturn(expected)

        val response = controller.createOrder(request, kioskId = "kiosk-1", userIdHeader = "7")

        assertEquals(HttpStatus.ACCEPTED, response.statusCode)
        assertEquals(expected, response.body)
    }

    @Test
    fun `createOrder with no user id header passes null userId`() {
        val request = OrderRequest(
            items = listOf(OrderItemRequest(itemId = 1L, quantity = 1)),
        )
        val expected = OrderResponse(orderId = 2L, status = OrderStatus.PROCESSING)

        `when`(createOrderUseCase.placeOrder(request, null, "kiosk-1")).thenReturn(expected)

        val response = controller.createOrder(request, kioskId = "kiosk-1", userIdHeader = null)

        assertEquals(HttpStatus.ACCEPTED, response.statusCode)
        assertEquals(expected, response.body)
    }

    @Test
    fun `createOrder uses default kiosk id when header is omitted`() {
        val request = OrderRequest(
            items = listOf(OrderItemRequest(itemId = 1L, quantity = 1)),
        )
        val expected = OrderResponse(orderId = 3L, status = OrderStatus.PROCESSING)

        `when`(createOrderUseCase.placeOrder(request, null, "1")).thenReturn(expected)

        val response = controller.createOrder(request, kioskId = "1", userIdHeader = null)

        assertEquals(HttpStatus.ACCEPTED, response.statusCode)
        assertEquals(expected, response.body)
    }

    @Test
    fun `createOrder propagates business exception`() {
        val request = OrderRequest(
            items = listOf(OrderItemRequest(itemId = 1L, quantity = 1)),
        )

        `when`(createOrderUseCase.placeOrder(request, 7L, "kiosk-1")).thenThrow(OrderTransactionFailedException())

        assertThrows<OrderTransactionFailedException> {
            controller.createOrder(request, kioskId = "kiosk-1", userIdHeader = "7")
        }
    }

    @Test
    fun `getOrder returns 200 with response body`() {
        val expected = OrderResponse(orderId = 1L, status = OrderStatus.COMPLETED)
        `when`(getOrderUseCase.getOrder(1L)).thenReturn(expected)

        val response = controller.getOrder(1L)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(expected, response.body)
    }

    @Test
    fun `streamOrder delegates current response to sse registry`() {
        val emitter = SseEmitter()
        `when`(orderSseRegistry.register(eq(1L), any())).thenReturn(emitter)

        val response = controller.streamOrder(1L)

        assertEquals(emitter, response)
        verify(orderSseRegistry).register(eq(1L), any())
    }

    @Test
    fun `cancelOrder uses default kiosk id when header is omitted`() {
        val expected = OrderResponse(orderId = 1L, status = OrderStatus.CANCEL_REQUESTED)
        `when`(cancelOrderUseCase.cancel(1L, "1")).thenReturn(expected)

        val response = controller.cancelOrder(1L, kioskId = "1")

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(expected, response.body)
    }

}
