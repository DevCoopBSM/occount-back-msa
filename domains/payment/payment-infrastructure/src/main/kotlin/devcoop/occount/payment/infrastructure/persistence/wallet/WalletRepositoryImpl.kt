package devcoop.occount.payment.infrastructure.persistence.wallet

import devcoop.occount.payment.application.output.WalletRepository
import devcoop.occount.payment.domain.wallet.Wallet
import org.springframework.stereotype.Repository

@Repository
class WalletRepositoryImpl(
    private val walletPersistenceRepository: WalletPersistenceRepository,
) : WalletRepository {
    override fun findByUserId(userId: Long): Wallet? {
        return walletPersistenceRepository.findById(userId)
            .map(WalletPersistenceMapper::toDomain)
            .orElse(null)
    }

    override fun save(wallet: Wallet): Wallet {
        return walletPersistenceRepository.save(WalletPersistenceMapper.toEntity(wallet))
            .let(WalletPersistenceMapper::toDomain)
    }
}
