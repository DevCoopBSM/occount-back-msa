package devcoop.occount.order.application.usecase.order.create

import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.core.common.event.OrderRequestedEvent
import devcoop.occount.core.common.event.OrderRequestedItemPayload
import devcoop.occount.order.application.exception.OrderCannotCancelException
import devcoop.occount.order.application.exception.OrderNotFoundException
import devcoop.occount.order.application.output.KioskActiveOrderRepository
import devcoop.occount.order.application.output.OrderRepository
import devcoop.occount.order.application.config.OrderTimeoutConfig
import devcoop.occount.order.application.shared.OrderRequest
import devcoop.occount.order.application.shared.OrderResponse
import devcoop.occount.order.application.support.OrderMutationExecutor
import devcoop.occount.order.application.usecase.order.cancel.CancelOrderUseCase
import devcoop.occount.order.domain.order.OrderAggregate
import devcoop.occount.order.domain.order.OrderPayment
import devcoop.occount.order.domain.order.OrderStatus
import devcoop.occount.order.domain.order.RequestedOrderLine
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CreateOrderUseCase(
    private val orderMutationExecutor: OrderMutationExecutor,
    private val orderRepository: OrderRepository,
    private val eventPublisher: EventPublisher,
    private val orderTimeoutConfig: OrderTimeoutConfig,
    private val kioskActiveOrderRepository: KioskActiveOrderRepository,
    private val cancelOrderUseCase: CancelOrderUseCase,
) {
    fun placeOrder(request: OrderRequest, userId: Long?, kioskId: String): OrderResponse {
        val requestedLines = request.items.map { RequestedOrderLine(itemId = it.itemId, quantity = it.quantity) }

        val createdOrder = orderMutationExecutor.executeInNewTransaction {
            val now = Instant.now()
            val createdOrder = orderRepository.save(
                OrderAggregate(
                    orderId = 0L,
                    userId = userId,
                    requestedLines = requestedLines,
                    payment = OrderPayment(
                        totalAmount = 0,
                    ),
                    status = OrderStatus.PROCESSING,
                    kioskId = kioskId,
                    expiresAt = now.plusSeconds(orderTimeoutConfig.timeoutSeconds),
                ),
            )
            publishOrderRequested(createdOrder, userId)
            createdOrder
        }
        log.info("주문 생성 완료 - 주문={} 사용자={}", createdOrder.orderId, userId)

        supersedePreviousKioskOrder(kioskId, createdOrder.orderId)

        return OrderResponse(orderId = createdOrder.orderId, status = createdOrder.status)
    }

    /**
     * 같은 키오스크에서 새 주문이 들어오면 손님이 새로 시작한 것이므로, 이전 진행 중 주문을 취소한다.
     * VAN 단말은 키오스크당 1대라 활성 주문도 1건이어야 이중결제가 나지 않는다.
     * 취소는 기존 [CancelOrderUseCase] 흐름을 재사용한다 → 결제 시작 전이면 VAN 청구를 건너뛰고
     * (`CANCELLED_BEFORE_START`), 이미 진행 중이면 단말 망취소가 요청된다(안전하게 구분 처리).
     */
    private fun supersedePreviousKioskOrder(kioskId: String, newOrderId: Long) {
        val displacedOrderId = kioskActiveOrderRepository.claimActiveOrder(kioskId, newOrderId)
            ?: return
        // supersede 는 best-effort 이다 — 이전 주문 취소가 실패해도 방금 생성된 신규 주문은 그대로 진행한다.
        try {
            cancelOrderUseCase.cancel(displacedOrderId, kioskId)
            log.info(
                "이전 키오스크 주문 취소(supersede) - 키오스크={} 취소주문={} 신규주문={}",
                kioskId,
                displacedOrderId,
                newOrderId,
            )
        } catch (_: OrderCannotCancelException) {
            // 이미 종료(완료/실패/취소) 상태면 취소할 것이 없음 → 무시.
        } catch (_: OrderNotFoundException) {
            // 정리 과정에서 사라진 주문이면 무시.
        } catch (ex: Exception) {
            log.warn(
                "이전 키오스크 주문 취소(supersede) 실패 - 키오스크={} 취소주문={} 신규주문={}",
                kioskId,
                displacedOrderId,
                newOrderId,
                ex,
            )
        }
    }

    private fun publishOrderRequested(createdOrder: OrderAggregate, userId: Long?) {
        eventPublisher.publish(
            topic = DomainTopics.ORDER_REQUESTED,
            key = createdOrder.orderId.toString(),
            eventType = DomainEventTypes.ORDER_REQUESTED,
            payload = OrderRequestedEvent(
                orderId = createdOrder.orderId,
                userId = userId,
                kioskId = createdOrder.kioskId,
                items = createdOrder.requestedLines.map { line ->
                    OrderRequestedItemPayload(
                        itemId = line.itemId,
                        quantity = line.quantity,
                    )
                },
            ),
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(CreateOrderUseCase::class.java)
    }
}
