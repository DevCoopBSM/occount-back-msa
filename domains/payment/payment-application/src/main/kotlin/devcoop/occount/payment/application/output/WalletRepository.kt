package devcoop.occount.payment.application.output

import devcoop.occount.payment.domain.wallet.Wallet

interface WalletRepository {
    fun findByUserId(userId: Long): Wallet?
    fun save(wallet: Wallet): Wallet
}
