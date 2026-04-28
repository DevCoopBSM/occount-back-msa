package devcoop.occount.payment.infrastructure.watchdog

import devcoop.occount.payment.application.output.OrderPaymentExecutionRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Component
@ConditionalOnProperty(prefix = "alerts", name = ["webhook-url"])
class StuckPaymentWatchdog(
    private val repository: OrderPaymentExecutionRepository,
    private val notifier: DiscordAlertNotifier,
    private val properties: StuckPaymentAlertProperties,
) {
    private val alertedAt = ConcurrentHashMap<Long, Instant>()

    @Scheduled(cron = "0 * * * * *")
    fun scan() {
        val now = Instant.now()
        evictExpired(now)

        val cutoff = LocalDateTime.now().minus(properties.threshold)
        val candidates = runCatching {
            repository.findStuckInProcessing(cutoff, properties.scanLimit)
        }.getOrElse { ex ->
            log.error("stuck 결제 조회 실패", ex)
            return
        }

        val newOnes = candidates.filter { id ->
            val last = alertedAt[id] ?: return@filter true
            now.isAfter(last.plus(properties.cooldown))
        }
        if (newOnes.isEmpty()) return

        newOnes.forEach { alertedAt[it] = now }
        notifier.send(buildMessage(newOnes))
        log.warn("결제 PROCESSING 잔류 감지 - count={} orderIds={}", newOnes.size, newOnes)
    }

    private fun buildMessage(orderIds: List<Long>): String {
        val thresholdMin = properties.threshold.toMinutes()
        val preview = orderIds.take(20).joinToString(", ")
        val suffix = if (orderIds.size > 20) " 외 ${orderIds.size - 20}건" else ""
        return ":warning: 결제 PROCESSING ${thresholdMin}분 이상 잔류 (${orderIds.size}건): $preview$suffix"
    }

    private fun evictExpired(now: Instant) {
        val expiry = properties.cooldown.multipliedBy(2)
        alertedAt.entries.removeIf { now.isAfter(it.value.plus(expiry)) }
    }

    companion object {
        private val log = LoggerFactory.getLogger(StuckPaymentWatchdog::class.java)
    }
}
