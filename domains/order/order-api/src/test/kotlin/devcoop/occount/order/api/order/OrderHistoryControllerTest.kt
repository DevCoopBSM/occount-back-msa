package devcoop.occount.order.api.order

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.core.common.auth.AuthPrincipalArgumentResolver
import devcoop.occount.order.api.sse.DefaultOrderSseEmitterSupport
import devcoop.occount.order.api.sse.OrderSseRegistry
import devcoop.occount.order.api.support.ApiAdviceHandler
import devcoop.occount.order.api.support.FakeEventPublisher
import devcoop.occount.order.api.support.FakeOrderRepository
import devcoop.occount.order.api.support.FakeOrderStatusNotifier
import devcoop.occount.order.api.support.FakeSalesRankingRepository
import devcoop.occount.order.api.support.FakeTransactionPort
import devcoop.occount.order.application.config.OrderTimeoutConfig
import devcoop.occount.order.application.query.OrderQueryService
import devcoop.occount.order.application.query.SalesRankingQueryParser
import devcoop.occount.order.application.query.SalesRankingQueryService
import devcoop.occount.order.application.support.OrderHistoryMapper
import devcoop.occount.order.application.support.OrderLifecycleProcessor
import devcoop.occount.order.application.support.OrderMutationExecutor
import devcoop.occount.order.application.support.OrderPaymentCancellationEventPublisher
import devcoop.occount.order.application.support.OrderResponseMapper
import devcoop.occount.order.application.support.OrderStreamEventMapper
import devcoop.occount.order.application.usecase.order.cancel.CancelOrderUseCase
import devcoop.occount.order.application.usecase.order.create.CreateOrderUseCase
import devcoop.occount.order.domain.order.OrderAggregate
import devcoop.occount.order.domain.order.OrderLine
import devcoop.occount.order.domain.order.OrderPayment
import devcoop.occount.order.domain.order.OrderPaymentResult
import devcoop.occount.order.domain.order.OrderStatus
import devcoop.occount.order.domain.order.OrderStepStatus
import org.junit.jupiter.api.Test
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.module.kotlin.jacksonMapperBuilder

class OrderHistoryControllerTest {
    @Test
    fun `getMyOrders returns 200 with snake_case orders including payment info`() {
        val mockMvc = buildMockMvc(
            initialOrders = listOf(historyOrder(orderId = 1L, userId = 7L)),
        )

        mockMvc.perform(
            get("/orders")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "7")
                .param("page", "0")
                .param("size", "10"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.orders.length()").value(1))
            .andExpect(jsonPath("$.total_count").value(1))
            .andExpect(jsonPath("$.current_page").value(0))
            .andExpect(jsonPath("$.page_size").value(10))
            .andExpect(jsonPath("$.orders[0].order_id").value(1))
            .andExpect(jsonPath("$.orders[0].status").value(OrderStatus.COMPLETED.name))
            .andExpect(jsonPath("$.orders[0].total_amount").value(4500))
            .andExpect(jsonPath("$.orders[0].lines[0].item_name_snapshot").value("삼각김밥"))
            .andExpect(jsonPath("$.orders[0].payment.payment_log_id").value(5001))
            .andExpect(jsonPath("$.orders[0].payment.points_used").value(500))
            .andExpect(jsonPath("$.orders[0].payment.card_amount").value(4000))
    }

    @Test
    fun `getMyOrders only returns the authenticated user orders`() {
        val mockMvc = buildMockMvc(
            initialOrders = listOf(
                historyOrder(orderId = 1L, userId = 7L),
                historyOrder(orderId = 2L, userId = 99L),
            ),
        )

        mockMvc.perform(
            get("/orders").header(AuthHeaders.AUTHENTICATED_USER_ID, "7"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.total_count").value(1))
            .andExpect(jsonPath("$.orders[0].order_id").value(1))
    }

    private fun historyOrder(orderId: Long, userId: Long): OrderAggregate =
        OrderAggregate(
            orderId = orderId,
            userId = userId,
            lines = listOf(
                OrderLine(itemId = 1L, itemNameSnapshot = "삼각김밥", unitPrice = 1500, quantity = 3, totalPrice = 4500),
            ),
            payment = OrderPayment(totalAmount = 4500),
            status = OrderStatus.COMPLETED,
            paymentStatus = OrderStepStatus.SUCCEEDED,
            kioskId = "kiosk-1",
            expiresAt = Instant.now().plusSeconds(30),
            paymentResult = OrderPaymentResult(
                paymentLogId = 5001L,
                pointsUsed = 500,
                cardAmount = 4000,
                transactionId = "TXN-1",
                approvalNumber = "00012345",
            ),
            createdAt = Instant.parse("2026-06-11T09:00:00Z"),
        )

    private fun buildMockMvc(initialOrders: List<OrderAggregate>): MockMvc {
        val orderRepository = FakeOrderRepository(initialOrders)
        val eventPublisher = FakeEventPublisher()
        val transactionPort = FakeTransactionPort()
        val controller = OrderController(
            createOrderUseCase = CreateOrderUseCase(
                orderMutationExecutor = OrderMutationExecutor(orderRepository, transactionPort),
                orderRepository = orderRepository,
                eventPublisher = eventPublisher,
                orderTimeoutConfig = OrderTimeoutConfig(timeoutSeconds = 30, asyncTimeoutBufferMillis = 1_000),
            ),
            cancelOrderUseCase = CancelOrderUseCase(
                orderMutationExecutor = OrderMutationExecutor(orderRepository, transactionPort),
                orderLifecycleProcessor = OrderLifecycleProcessor(
                    orderStatusNotifier = FakeOrderStatusNotifier(),
                    orderStreamEventMapper = OrderStreamEventMapper(),
                ),
                orderPaymentCancellationEventPublisher = OrderPaymentCancellationEventPublisher(eventPublisher),
                orderResponseMapper = OrderResponseMapper(),
            ),
            orderQueryService = OrderQueryService(
                orderRepository = orderRepository,
                orderResponseMapper = OrderResponseMapper(),
                orderStreamEventMapper = OrderStreamEventMapper(),
                orderHistoryMapper = OrderHistoryMapper(),
            ),
            salesRankingQueryService = SalesRankingQueryService(
                salesRankingRepository = FakeSalesRankingRepository(),
                salesRankingQueryParser = SalesRankingQueryParser(),
            ),
            orderSseRegistry = OrderSseRegistry(DefaultOrderSseEmitterSupport()),
        )
        val objectMapper = jacksonMapperBuilder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build()
        return MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(ApiAdviceHandler())
            .setCustomArgumentResolvers(
                AuthPrincipalArgumentResolver(),
                PageableHandlerMethodArgumentResolver(),
            )
            .setMessageConverters(JacksonJsonHttpMessageConverter(objectMapper))
            .build()
    }
}
