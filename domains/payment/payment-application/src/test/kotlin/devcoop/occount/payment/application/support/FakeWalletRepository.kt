package devcoop.occount.payment.application.support

import devcoop.occount.payment.application.output.WalletRepository
import devcoop.occount.payment.domain.Wallet

class FakeWalletRepository(
    private val wallets: MutableMap<Long, Wallet> = mutableMapOf(),
    private val saveException: RuntimeException? = null,
) : WalletRepository {
    val savedWallets = mutableListOf<Wallet>()

    override fun findByUserId(userId: Long): Wallet? = wallets[userId]

    override fun save(pointWallet: Wallet): Wallet {
        saveException?.let { throw it }
        wallets[pointWallet.userId] = pointWallet
        savedWallets += pointWallet
        return pointWallet
    }
}
