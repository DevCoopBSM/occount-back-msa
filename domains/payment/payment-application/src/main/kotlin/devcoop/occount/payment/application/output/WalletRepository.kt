package devcoop.occount.payment.application.output

import devcoop.occount.payment.domain.Wallet

interface WalletRepository {
    fun findByUserId(userId: Long): Wallet?
    fun save(pointWallet: Wallet): Wallet
}
