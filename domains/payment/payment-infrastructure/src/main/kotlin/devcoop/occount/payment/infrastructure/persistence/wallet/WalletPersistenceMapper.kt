package devcoop.occount.payment.infrastructure.persistence.wallet

import devcoop.occount.payment.domain.wallet.Wallet

object WalletPersistenceMapper {
    fun toDomain(entity: WalletJpaEntity): Wallet {
        return Wallet(
            userId = entity.getUserId(),
            point = entity.getBalance(),
        )
    }

    fun toEntity(domain: Wallet): WalletJpaEntity {
        return WalletJpaEntity(
            userId = domain.userId,
            point = domain.point,
        )
    }
}
