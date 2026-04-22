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
            repeat(3) {
                walletRepository.findByUserId(-1L)
                paymentLogRepository.findByUserId(-1L)
            }
        }
        log.info("Payment business warmup completed in {} ms", elapsed)
    }

    companion object {
        private val log = LoggerFactory.getLogger(PaymentBusinessWarmup::class.java)
    }
}
