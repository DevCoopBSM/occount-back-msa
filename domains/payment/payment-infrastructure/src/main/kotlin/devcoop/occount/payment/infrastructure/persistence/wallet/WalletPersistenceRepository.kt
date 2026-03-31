package devcoop.occount.payment.infrastructure.persistence.wallet

import org.springframework.data.jpa.repository.JpaRepository

interface WalletPersistenceRepository : JpaRepository<WalletJpaEntity, Long>
