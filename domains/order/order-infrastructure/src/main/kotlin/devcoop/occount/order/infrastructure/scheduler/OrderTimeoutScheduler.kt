package devcoop.occount.order.infrastructure.scheduler

import devcoop.occount.order.application.usecase.order.timeout.ExpireTimedOutOrdersUseCase
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class OrderTimeoutScheduler(
    private val expireTimedOutOrdersUseCase: ExpireTimedOutOrdersUseCase,
) {
    @Scheduled(fixedDelayString = "\${order.timeout-scan-fixed-delay-millis:1000}")
    fun expireTimedOutOrders() {
        expireTimedOutOrdersUseCase.expire()
    }
}
