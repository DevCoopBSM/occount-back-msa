package devcoop.occount.order.api.order

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.order.api.sse.OrderSseRegistry
import devcoop.occount.order.application.exception.OrderTransactionFailedException
import devcoop.occount.order.application.shared.OrderItemRequest
import devcoop.occount.order.application.shared.OrderRequest
import devcoop.occount.order.application.shared.OrderResponse
import devcoop.occount.order.application.shared.OrderStreamEvent
import devcoop.occount.order.application.shared.OrderStreamEventType
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
        val expected = OrderResponse(orderId = "order-1", status = OrderStatus.PROCESSING)

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
        val expected = OrderResponse(orderId = "order-2", status = OrderStatus.PROCESSING)

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
        val expected = OrderResponse(orderId = "order-3", status = OrderStatus.PROCESSING)

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
        val expected = OrderResponse(orderId = "order-1", status = OrderStatus.COMPLETED)
        `when`(getOrderUseCase.getOrder("order-1")).thenReturn(expected)

        val response = controller.getOrder("order-1")

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(expected, response.body)
    }

    @Test
    fun `streamOrder delegates current response to sse registry`() {
        val current = OrderStreamEvent(
            type = OrderStreamEventType.ORDER_ACCEPTED,
            payload = OrderResponse(orderId = "order-1", status = OrderStatus.PROCESSING),
        )
        val emitter = SseEmitter()
        `when`(getOrderUseCase.getOrderStreamEvent("order-1")).thenReturn(current)
        `when`(orderSseRegistry.register(current)).thenReturn(emitter)

        val response = controller.streamOrder("order-1")

        assertEquals(emitter, response)
        verify(orderSseRegistry).register(current)
    }

    @Test
    fun `cancelOrder uses default kiosk id when header is omitted`() {
        val expected = OrderResponse(orderId = "order-1", status = OrderStatus.CANCEL_REQUESTED)
        `when`(cancelOrderUseCase.cancel("order-1", "1")).thenReturn(expected)

        val response = controller.cancelOrder("order-1", kioskId = "1")

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(expected, response.body)
    }
}
