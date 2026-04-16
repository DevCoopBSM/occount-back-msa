package devcoop.occount.order.infrastructure.transaction

import devcoop.occount.order.application.exception.OrderConcurrencyException
import devcoop.occount.order.application.output.TransactionPort
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

@Component
class SpringTransactionAdapter(transactionManager: PlatformTransactionManager) : TransactionPort {
    private val template = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    override fun <T : Any> executeInNewTransaction(action: () -> T): T {
        return try {
            template.execute { action() }
        } catch (ex: OptimisticLockingFailureException) {
            throw OrderConcurrencyException()
        }
    }
}
