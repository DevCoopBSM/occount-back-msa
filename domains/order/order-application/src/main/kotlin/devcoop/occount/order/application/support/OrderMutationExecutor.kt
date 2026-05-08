package devcoop.occount.order.application.support

import devcoop.occount.order.application.exception.DuplicateEventException
import devcoop.occount.order.application.exception.OrderConcurrencyException
import devcoop.occount.order.application.exception.OrderNotFoundException
import devcoop.occount.order.application.exception.OrderTransactionFailedException
import devcoop.occount.order.application.output.OrderRepository
import devcoop.occount.order.application.output.TransactionPort
import devcoop.occount.order.domain.order.OrderAggregate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OrderMutationExecutor(
    private val orderRepository: OrderRepository,
    private val transactionPort: TransactionPort,
) {
    fun <T : Any> executeInNewTransaction(action: () -> T): T {
        return transactionPort.executeInNewTransaction(action)
    }

    fun save(order: OrderAggregate): OrderAggregate {
        return executeInNewTransaction {
            orderRepository.save(order)
        }
    }

    fun updateOrder(
        orderId: Long,
        update: (OrderAggregate) -> OrderAggregate,
        afterUpdate: (OrderAggregate) -> Unit = {},
    ): OrderAggregate {
        repeat(OrderRetryPolicy.MAX_RETRY_COUNT) { attempt ->
            try {
                return executeInNewTransaction {
                    val persistedOrder = orderRepository.findPersistedById(orderId)
                        ?: throw OrderNotFoundException()
                    val reconciledOrder = update(persistedOrder.order).reconcileStatus()

                    val updatedOrder = if (reconciledOrder == persistedOrder.order) {
                        persistedOrder.order
                    } else {
                        orderRepository.save(
                            reconciledOrder,
                            persistedOrder.persistenceVersion,
                        )
                    }
                    afterUpdate(updatedOrder)
                    updatedOrder
                }
            } catch (ex: OrderConcurrencyException) {
                log.warn("낙관적 락 충돌 - 주문={} 시도={}", orderId, attempt)
                if (attempt == OrderRetryPolicy.MAX_RETRY_COUNT - 1) {
                    throw OrderTransactionFailedException()
                }
                Thread.sleep(OrderRetryPolicy.BASE_BACKOFF_MILLIS * (1L shl attempt))
            }
        }

        throw OrderTransactionFailedException()
    }

    /**
     * 이벤트 멱등성 보장이 필요한 경우 사용.
     * [recordConsumption]이 [executeInNewTransaction] 내부에서 실행되어
     * 멱등성 키 삽입과 주문 업데이트가 동일 트랜잭션에서 원자적으로 처리된다.
     *
     * @return 업데이트된 주문. 이미 처리된 이벤트(중복)면 null 반환.
     */
    fun updateOrderIdempotently(
        orderId: Long,
        recordConsumption: () -> Unit,
        update: (OrderAggregate) -> OrderAggregate,
    ): OrderAggregate? {
        repeat(OrderRetryPolicy.MAX_RETRY_COUNT) { attempt ->
            try {
                return executeInNewTransaction {
                    recordConsumption()
                    val persistedOrder = orderRepository.findPersistedById(orderId)
                        ?: throw OrderNotFoundException()
                    val reconciledOrder = update(persistedOrder.order).reconcileStatus()

                    if (reconciledOrder == persistedOrder.order) {
                        persistedOrder.order
                    } else {
                        orderRepository.save(reconciledOrder, persistedOrder.persistenceVersion)
                    }
                }
            } catch (_: DuplicateEventException) {
                // 멱등성 키 중복 — 이미 처리된 이벤트
                return null
            } catch (ex: OrderConcurrencyException) {
                log.warn("낙관적 락 충돌 (멱등) - 주문={} 시도={}", orderId, attempt)
                if (attempt == OrderRetryPolicy.MAX_RETRY_COUNT - 1) throw OrderTransactionFailedException()
                Thread.sleep(OrderRetryPolicy.BASE_BACKOFF_MILLIS * (1L shl attempt))
            }
        }

        throw OrderTransactionFailedException()
    }

    companion object {
        private val log = LoggerFactory.getLogger(OrderMutationExecutor::class.java)
    }
}
