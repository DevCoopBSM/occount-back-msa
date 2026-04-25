package devcoop.occount.order.bootstrap.warmup

import devcoop.occount.order.application.output.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import java.time.Instant
import kotlin.system.measureTimeMillis

@Component
class OrderBusinessWarmup(
    private val orderRepository: OrderRepository,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        val elapsed = measureTimeMillis {
            repeat(JIT_WARMUP_COUNT) {
                orderRepository.findById(0L)
                orderRepository.findExpiredNonFinalOrderIds(Instant.now())
            }
        }
        log.info("Order business warmup completed ({} rounds) in {} ms", JIT_WARMUP_COUNT, elapsed)
    }

    companion object {
        private const val JIT_WARMUP_COUNT = 10
        private val log = LoggerFactory.getLogger(OrderBusinessWarmup::class.java)
    }
}
