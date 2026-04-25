package devcoop.occount.order.api.order

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.order.api.sse.DefaultOrderSseEmitterSupport
import devcoop.occount.order.api.sse.OrderSseRegistry
import devcoop.occount.order.api.support.FakeEventPublisher
import devcoop.occount.order.api.support.FakeOrderRepository
import devcoop.occount.order.api.support.FakeOrderStatusNotifier
import devcoop.occount.order.api.support.FakeTransactionPort
import devcoop.occount.order.api.support.mockMvc
import devcoop.occount.order.api.support.orderFixture
import devcoop.occount.order.application.config.OrderTimeoutConfig
import devcoop.occount.order.application.support.OrderCompensationScheduler
import devcoop.occount.order.application.support.OrderLifecycleProcessor
import devcoop.occount.order.application.support.OrderMutationExecutor
import devcoop.occount.order.application.support.OrderPaymentCancellationEventPublisher
import devcoop.occount.order.application.support.OrderResponseMapper
import devcoop.occount.order.application.support.OrderStreamEventMapper
import devcoop.occount.order.application.usecase.order.cancel.CancelOrderUseCase
import devcoop.occount.order.application.usecase.order.create.CreateOrderUseCase
import devcoop.occount.order.application.usecase.order.get.GetOrderUseCase
import devcoop.occount.order.domain.order.OrderStatus
import org.hamcrest.Matchers.containsString
import org.springframework.test.web.servlet.MockMvc
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("OrderController 웹 테스트")
class OrderControllerTest {
    @Test
    @DisplayName("주문 생성 요청이 성공하면 202와 주문 상태를 반환한다")
    fun `createOrder returns 202 Accepted on success`() {
        val mockMvc = buildMockMvc()

        mockMvc.perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header(AuthHeaders.KIOSK_ID, "kiosk-1")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "7")
                .content(
                    """
                    {
                      "items": [
                        {
                          "itemId": 1,
                          "quantity": 2
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
        ).andExpect(status().isAccepted)
            .andExpect(jsonPath("$.orderId").value(1))
            .andExpect(jsonPath("$.status").value(OrderStatus.PROCESSING.name))
    }

    @Test
    @DisplayName("주문 생성 요청은 orderInfos 별칭도 허용한다")
    fun `createOrder accepts orderInfos alias`() {
        val mockMvc = buildMockMvc()

        mockMvc.perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "orderInfos": [
                        {
                          "itemId": 1,
                          "quantity": 1
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
        ).andExpect(status().isAccepted)
            .andExpect(jsonPath("$.orderId").value(1))
            .andExpect(jsonPath("$.status").value(OrderStatus.PROCESSING.name))
    }

    @Test
    @DisplayName("주문 조회 요청이 성공하면 200과 주문 상태를 반환한다")
    fun `getOrder returns 200 with order response`() {
        val mockMvc = buildMockMvc(
            initialOrders = listOf(
                orderFixture(orderId = 1L, status = OrderStatus.COMPLETED),
            ),
        )

        mockMvc.perform(get("/orders/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.orderId").value(1))
            .andExpect(jsonPath("$.status").value(OrderStatus.COMPLETED.name))
    }

    @Test
    @DisplayName("없는 주문을 조회하면 404를 반환한다")
    fun `getOrder returns 404 when order does not exist`() {
        val mockMvc = buildMockMvc()

        mockMvc.perform(get("/orders/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("주문 정보를 찾을 수 없습니다."))
    }

    @Test
    @DisplayName("최종 상태의 SSE 구독은 현재 이벤트를 즉시 반환한다")
    fun `streamOrder returns current terminal event`() {
        val mockMvc = buildMockMvc(
            initialOrders = listOf(
                orderFixture(orderId = 1L, status = OrderStatus.COMPLETED),
            ),
        )

        val asyncResult = mockMvc.perform(get("/orders/1/stream"))
            .andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(asyncResult))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
            .andExpect(content().string(containsString("event:COMPLETED")))
    }

    @Test
    @DisplayName("주문 취소 요청은 키오스크 헤더가 없으면 기본값 1을 사용한다")
    fun `cancelOrder uses default kiosk header`() {
        val mockMvc = buildMockMvc(
            initialOrders = listOf(
                orderFixture(orderId = 1L, kioskId = "1"),
            ),
        )

        mockMvc.perform(post("/orders/1/cancel"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.orderId").value(1))
            .andExpect(jsonPath("$.status").value(OrderStatus.CANCEL_REQUESTED.name))
    }

    @Test
    @DisplayName("다른 키오스크가 주문 취소를 시도하면 403을 반환한다")
    fun `cancelOrder returns 403 when kiosk does not own order`() {
        val mockMvc = buildMockMvc(
            initialOrders = listOf(
                orderFixture(orderId = 1L, kioskId = "kiosk-1"),
            ),
        )

        mockMvc.perform(
            post("/orders/1/cancel")
                .header(AuthHeaders.KIOSK_ID, "kiosk-2"),
        ).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("해당 주문에 접근할 수 없습니다."))
    }

    private fun buildMockMvc(initialOrders: List<devcoop.occount.order.domain.order.OrderAggregate> = emptyList()): MockMvc {
        val orderRepository = FakeOrderRepository(initialOrders)
        val eventPublisher = FakeEventPublisher()
        val transactionPort = FakeTransactionPort()
        return mockMvc(
            OrderController(
                createOrderUseCase = CreateOrderUseCase(
                    orderMutationExecutor = OrderMutationExecutor(orderRepository, transactionPort),
                    orderRepository = orderRepository,
                    eventPublisher = eventPublisher,
                    orderTimeoutConfig = OrderTimeoutConfig(timeoutSeconds = 30, asyncTimeoutBufferMillis = 1_000),
                ),
                cancelOrderUseCase = CancelOrderUseCase(
                    orderMutationExecutor = OrderMutationExecutor(orderRepository, transactionPort),
                    orderLifecycleProcessor = OrderLifecycleProcessor(
                        orderCompensationScheduler = OrderCompensationScheduler(
                            orderRepository = orderRepository,
                            eventPublisher = eventPublisher,
                            transactionPort = transactionPort,
                        ),
                        orderStatusNotifier = FakeOrderStatusNotifier(),
                        orderStreamEventMapper = OrderStreamEventMapper(),
                    ),
                    orderPaymentCancellationEventPublisher = OrderPaymentCancellationEventPublisher(eventPublisher),
                    orderResponseMapper = OrderResponseMapper(),
                ),
                getOrderUseCase = GetOrderUseCase(
                    orderRepository = orderRepository,
                    orderResponseMapper = OrderResponseMapper(),
                    orderStreamEventMapper = OrderStreamEventMapper(),
                ),
                orderSseRegistry = OrderSseRegistry(DefaultOrderSseEmitterSupport()),
            ),
        )
    }
}
