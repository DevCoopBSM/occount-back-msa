package devcoop.occount.payment.bootstrap.warmup

import devcoop.occount.payment.application.output.PaymentLogRepository
import devcoop.occount.payment.application.output.WalletRepository
import devcoop.occount.warmup.BusinessWarmup
import devcoop.occount.warmup.WarmupProbe
import org.springframework.stereotype.Component

@Component
class PaymentBusinessWarmup(
    private val walletRepository: WalletRepository,
    private val paymentLogRepository: PaymentLogRepository,
) : BusinessWarmup {

    override fun warmup() {
        walletRepository.findByUserId(WarmupProbe.USER_ID)
        paymentLogRepository.findByUserId(WarmupProbe.USER_ID)
    }
}
