package devcoop.occount.order.bootstrap.warmup

import devcoop.occount.order.application.output.OrderRepository
import devcoop.occount.warmup.BusinessWarmup
import devcoop.occount.warmup.WarmupProbe
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class OrderBusinessWarmup(
    private val orderRepository: OrderRepository,
) : BusinessWarmup {

    override fun warmup() {
        orderRepository.findById(WarmupProbe.USER_ID)
        orderRepository.findExpiredNonFinalOrderIds(Instant.now())
    }
}
