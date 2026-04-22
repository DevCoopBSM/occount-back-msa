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
            repeat(3) {
                orderRepository.findById("00000000-0000-0000-0000-000000000000")
                orderRepository.findExpiredNonFinalOrderIds(Instant.now())
            }
        }
        log.info("Order business warmup completed in {} ms", elapsed)
    }

    companion object {
        private val log = LoggerFactory.getLogger(OrderBusinessWarmup::class.java)
    }
}
