package devcoop.occount.member.infrastructure.persistence

import devcoop.occount.member.domain.user.User
import devcoop.occount.member.application.output.UserRepository
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryImpl(
    private val userJpaRepository: UserJpaRepository
) : UserRepository {
    override fun findById(id: Long): User? {
        return userJpaRepository.findById(id)
            .map(UserPersistenceMapper::toDomain)
            .orElse(null)
    }

    override fun findByUserBarcode(userBarcode: String): User? {
        return userJpaRepository.findByUserBarcode(userBarcode)
            ?.let(UserPersistenceMapper::toDomain)
    }

    override fun findByEmail(userEmail: String): User? {
        return userJpaRepository.findByEmail(userEmail)
            ?.let(UserPersistenceMapper::toDomain)
    }

    override fun existsByEmail(userEmail: String): Boolean {
        return userJpaRepository.existsByEmail(userEmail)
    }

    override fun save(user: User): User {
        return userJpaRepository.save(UserPersistenceMapper.toEntity(user))
            .let(UserPersistenceMapper::toDomain)
    }
}
