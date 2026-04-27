package devcoop.occount.warmup

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import kotlin.system.measureTimeMillis

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
@ConditionalOnProperty(
    prefix = "app.startup-warmup",
    name = ["enabled", "business-enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class BusinessWarmupRunner(
    private val warmups: List<BusinessWarmup>,
    private val properties: StartupWarmupProperties,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        if (warmups.isEmpty()) {
            return
        }

        val rounds = properties.businessRepeat.coerceAtLeast(1)

        val elapsed = measureTimeMillis {
            repeat(rounds) {
                warmups.forEach { warmup ->
                    runCatching { warmup.warmup() }.onFailure { exception ->
                        log.warn("Business warmup failed for {}", warmup::class.java.simpleName, exception)
                    }
                }
            }
        }

        log.info(
            "Business warmup completed ({} runners x {} rounds) in {} ms",
            warmups.size, rounds, elapsed,
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(BusinessWarmupRunner::class.java)
    }
}
