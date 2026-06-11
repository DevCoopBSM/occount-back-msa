package devcoop.occount.member.infrastructure.login

import devcoop.occount.member.application.login.KioskLoginAttempt
import devcoop.occount.member.application.output.KioskLoginAttemptRepository
import org.springframework.stereotype.Repository

@Repository
class KioskLoginAttemptRepositoryImpl(
    private val kioskLoginAttemptJpaRepository: KioskLoginAttemptJpaRepository,
) : KioskLoginAttemptRepository {

    override fun findByBarcode(userBarcode: String): KioskLoginAttempt? {
        return kioskLoginAttemptJpaRepository.findByUserBarcode(userBarcode)?.toDomain()
    }

    override fun save(attempt: KioskLoginAttempt): KioskLoginAttempt {
        return kioskLoginAttemptJpaRepository.save(attempt.toEntity()).toDomain()
    }

    override fun deleteByBarcode(userBarcode: String) {
        kioskLoginAttemptJpaRepository.deleteById(userBarcode)
    }

    private fun KioskLoginAttempt.toEntity() = KioskLoginAttemptJpaEntity(
        userBarcode = userBarcode,
        failCount = failCount,
        lockedUntil = lockedUntil,
        updatedAt = updatedAt,
    )

    private fun KioskLoginAttemptJpaEntity.toDomain() = KioskLoginAttempt(
        userBarcode = userBarcode,
        failCount = failCount,
        lockedUntil = lockedUntil,
        updatedAt = updatedAt,
    )
}
