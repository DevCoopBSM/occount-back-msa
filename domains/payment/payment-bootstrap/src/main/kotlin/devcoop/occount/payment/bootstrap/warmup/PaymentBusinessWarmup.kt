package devcoop.occount.payment.bootstrap.warmup

import devcoop.occount.payment.application.output.PaymentLogRepository
import devcoop.occount.payment.application.output.WalletRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import kotlin.system.measureTimeMillis

@Component
class PaymentBusinessWarmup(
    private val walletRepository: WalletRepository,
    private val paymentLogRepository: PaymentLogRepository,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        val elapsed = measureTimeMillis {
            repeat(JIT_WARMUP_COUNT) {
                walletRepository.findByUserId(-1L)
                paymentLogRepository.findByUserId(-1L)
            }
        }
        log.info("Payment business warmup completed ({} rounds) in {} ms", JIT_WARMUP_COUNT, elapsed)
    }

    companion object {
        private const val JIT_WARMUP_COUNT = 10
        private val log = LoggerFactory.getLogger(PaymentBusinessWarmup::class.java)
    }
}
