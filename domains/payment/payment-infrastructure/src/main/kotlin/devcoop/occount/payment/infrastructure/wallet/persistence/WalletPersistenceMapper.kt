package devcoop.occount.payment.infrastructure.wallet.persistence

import devcoop.occount.payment.domain.Wallet

object WalletPersistenceMapper {
    fun toDomain(entity: WalletJpaEntity): Wallet {
        return Wallet(
            userId = entity.getUserId(),
            balance = entity.getBalance(),
        )
    }

    fun toEntity(domain: Wallet): WalletJpaEntity {
        return WalletJpaEntity(
            userId = domain.userId,
            balance = domain.balance,
        )
    }
}
