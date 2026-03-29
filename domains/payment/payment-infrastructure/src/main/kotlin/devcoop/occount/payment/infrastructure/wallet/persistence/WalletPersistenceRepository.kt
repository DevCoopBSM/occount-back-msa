package devcoop.occount.payment.infrastructure.wallet.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface WalletPersistenceRepository : JpaRepository<WalletJpaEntity, Long>
