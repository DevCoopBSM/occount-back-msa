package devcoop.occount.order.application.support

import devcoop.occount.order.application.output.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class CompensationSweeper(
    private val orderRepository: OrderRepository,
    private val orderCompensationScheduler: OrderCompensationScheduler,
    @param:Value("\${order.compensation-sweeper.batch-size:100}") private val batchSize: Int,
) {
    @Scheduled(fixedDelayString = "\${order.compensation-sweeper.fixed-delay-ms:1000}")
    fun sweep() {
        val orderIds = orderRepository.findOrderIdsRequiringCompensation(batchSize)
        if (orderIds.isEmpty()) return
        orderIds.forEach { orderId ->
            runCatching {
                orderCompensationScheduler.scheduleRequiredCompensations(orderId)
            }.onFailure { log.warn("보상 스위핑 실패 - 주문={}", orderId, it) }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(CompensationSweeper::class.java)
    }
}
