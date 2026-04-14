package devcoop.occount.order.application.support

import devcoop.occount.order.application.exception.OrderNotFoundException
import devcoop.occount.order.application.exception.OrderTransactionFailedException
import devcoop.occount.order.application.output.OrderRepository
import devcoop.occount.order.domain.order.OrderAggregate
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

@Component
class OrderMutationExecutor(
    private val orderRepository: OrderRepository,
    transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    fun <T : Any> executeInNewTransaction(action: () -> T): T {
        return transactionTemplate.execute {
            action()
        }
    }

    fun save(order: OrderAggregate): OrderAggregate {
        return executeInNewTransaction {
            orderRepository.save(order)
        }
    }

    fun updateOrder(
        orderId: String,
        update: (OrderAggregate) -> OrderAggregate,
    ): OrderAggregate {
        repeat(MAX_RETRY_COUNT) { attempt ->
            try {
                return executeInNewTransaction {
                    val persistedOrder = orderRepository.findPersistedById(orderId)
                        ?: throw OrderNotFoundException()
                    val reconciledOrder = update(persistedOrder.order).reconcileStatus()

                    if (reconciledOrder == persistedOrder.order) {
                        persistedOrder.order
                    } else {
                        orderRepository.save(
                            reconciledOrder,
                            persistedOrder.persistenceVersion,
                        )
                    }
                }
            } catch (ex: OptimisticLockingFailureException) {
                log.warn("낙관적 락 충돌 - 주문={} 시도={}", orderId, attempt)
                if (attempt == MAX_RETRY_COUNT - 1) {
                    throw ex
                }
                Thread.sleep(BASE_BACKOFF_MILLIS * (1L shl attempt))
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
        orderId: String,
        recordConsumption: () -> Unit,
        update: (OrderAggregate) -> OrderAggregate,
    ): OrderAggregate? {
        repeat(MAX_RETRY_COUNT) { attempt ->
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
            } catch (_: DataIntegrityViolationException) {
                // 멱등성 키 중복 — 이미 처리된 이벤트
                return null
            } catch (ex: OptimisticLockingFailureException) {
                log.warn("낙관적 락 충돌 (멱등) - 주문={} 시도={}", orderId, attempt)
                if (attempt == MAX_RETRY_COUNT - 1) throw ex
                Thread.sleep(BASE_BACKOFF_MILLIS * (1L shl attempt))
            }
        }

        throw OrderTransactionFailedException()
    }

    companion object {
        private val log = LoggerFactory.getLogger(OrderMutationExecutor::class.java)
        private const val MAX_RETRY_COUNT = 3
        private const val BASE_BACKOFF_MILLIS = 50L
    }
}
