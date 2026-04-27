package devcoop.occount.payment.infrastructure.persistence.execution

import devcoop.occount.payment.application.output.OrderPaymentCancellationRequestResult
import devcoop.occount.payment.application.output.OrderPaymentExecutionRepository
import devcoop.occount.payment.application.output.OrderPaymentExecutionStartResult
import devcoop.occount.payment.application.output.OrderPaymentExecutionState
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class OrderPaymentExecutionRepositoryImpl(
    private val persistenceRepository: OrderPaymentExecutionPersistenceRepository,
) : OrderPaymentExecutionRepository {
    override fun startProcessing(orderId: Long): OrderPaymentExecutionStartResult {
        val execution = persistenceRepository.findById(orderId).orElse(null)
        if (execution == null) {
            persistenceRepository.save(
                OrderPaymentExecutionJpaEntity(
                    orderId = orderId,
                    state = OrderPaymentExecutionState.PROCESSING,
                    cancellationRequested = false,
                ),
            )
            return OrderPaymentExecutionStartResult.STARTED
        }

        return when (execution.getState()) {
            OrderPaymentExecutionState.CANCELLATION_REQUESTED -> OrderPaymentExecutionStartResult.CANCELLED_BEFORE_START
            OrderPaymentExecutionState.PROCESSING,
            OrderPaymentExecutionState.COMPLETED,
            OrderPaymentExecutionState.FAILED,
            OrderPaymentExecutionState.CANCELLED,
            -> OrderPaymentExecutionStartResult.DUPLICATE
        }
    }

    @Transactional
    override fun requestCancellation(orderId: Long): OrderPaymentCancellationRequestResult {
        val execution = persistenceRepository.findByOrderIdForUpdate(orderId)
        if (execution == null) {
            persistenceRepository.save(
                OrderPaymentExecutionJpaEntity(
                    orderId = orderId,
                    state = OrderPaymentExecutionState.CANCELLATION_REQUESTED,
                    cancellationRequested = true,
                ),
            )
            return OrderPaymentCancellationRequestResult.NO_ACTIVE_PAYMENT
        }

        return when (execution.getState()) {
            OrderPaymentExecutionState.PROCESSING -> {
                execution.markCancellationRequested()
                OrderPaymentCancellationRequestResult.TERMINAL_CANCELLATION_REQUIRED
            }

            OrderPaymentExecutionState.CANCELLATION_REQUESTED,
            OrderPaymentExecutionState.CANCELLED,
            OrderPaymentExecutionState.COMPLETED,
            OrderPaymentExecutionState.FAILED,
            -> OrderPaymentCancellationRequestResult.NO_ACTIVE_PAYMENT
        }
    }

    @Transactional(readOnly = true)
    override fun isCancellationRequested(orderId: Long): Boolean {
        return persistenceRepository.findById(orderId)
            .map(OrderPaymentExecutionJpaEntity::isCancellationRequested)
            .orElse(false)
    }

    override fun markCompleted(orderId: Long) {
        upsertState(orderId, OrderPaymentExecutionState.COMPLETED, false)
    }

    override fun markFailed(orderId: Long) {
        upsertState(orderId, OrderPaymentExecutionState.FAILED, false)
    }

    override fun markCancelled(orderId: Long) {
        upsertState(orderId, OrderPaymentExecutionState.CANCELLED, true)
    }

    private fun upsertState(
        orderId: Long,
        state: OrderPaymentExecutionState,
        cancellationRequested: Boolean,
    ) {
        val execution = persistenceRepository.findById(orderId).orElse(null)
        if (execution == null) {
            persistenceRepository.save(
                OrderPaymentExecutionJpaEntity(
                    orderId = orderId,
                    state = state,
                    cancellationRequested = cancellationRequested,
                ),
            )
            return
        }

        execution.changeState(state)
        if (cancellationRequested) {
            execution.markCancellationRequested()
        }
    }
}
