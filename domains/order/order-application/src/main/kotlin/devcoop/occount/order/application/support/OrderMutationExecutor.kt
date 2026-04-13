package devcoop.occount.order.application.support

import devcoop.occount.order.application.exception.OrderNotFoundException
import devcoop.occount.order.application.exception.OrderTransactionFailedException
import devcoop.occount.order.application.output.OrderRepository
import devcoop.occount.order.domain.order.OrderAggregate
import org.slf4j.LoggerFactory
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

    companion object {
        private val log = LoggerFactory.getLogger(OrderMutationExecutor::class.java)
        private const val MAX_RETRY_COUNT = 3
        private const val BASE_BACKOFF_MILLIS = 50L
    }
}
