package devcoop.occount.payment.infrastructure.wallet.persistence

import devcoop.occount.payment.application.output.WalletRepository
import devcoop.occount.payment.domain.Wallet
import org.springframework.stereotype.Repository

@Repository
class WalletRepositoryImpl(
    private val pointWalletPersistenceRepository: WalletPersistenceRepository,
) : WalletRepository {
    override fun findByUserId(userId: Long): Wallet? {
        return pointWalletPersistenceRepository.findById(userId)
            .map(WalletPersistenceMapper::toDomain)
            .orElse(null)
    }

    override fun save(pointWallet: Wallet): Wallet {
        return pointWalletPersistenceRepository.save(WalletPersistenceMapper.toEntity(pointWallet))
            .let(WalletPersistenceMapper::toDomain)
    }
}
