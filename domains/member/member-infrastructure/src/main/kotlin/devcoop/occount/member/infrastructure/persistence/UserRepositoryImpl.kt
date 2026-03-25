package devcoop.occount.member.infrastructure.persistence

import devcoop.occount.member.domain.user.User
import devcoop.occount.member.application.user.UserRepository
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryImpl(
    private val userPersistenceRepository: UserPersistenceRepository
) : UserRepository {
    override fun findById(id: Long): User? {
        return userPersistenceRepository.findById(id)
            .map(UserPersistenceMapper::toDomain)
            .orElse(null)
    }

    override fun findByUserBarcode(userBarcode: String): User? {
        return userPersistenceRepository.findByUserInfoUserBarcode(userBarcode)
            ?.let(UserPersistenceMapper::toDomain)
    }

    override fun findByUserEmail(userEmail: String): User? {
        return userPersistenceRepository.findByUserAccountInfoUserEmail(userEmail)
            ?.let(UserPersistenceMapper::toDomain)
    }

    override fun existsByUserEmail(userEmail: String): Boolean {
        return userPersistenceRepository.existsByUserAccountInfoUserEmail(userEmail)
    }

    override fun save(user: User): User {
        return userPersistenceRepository.save(UserPersistenceMapper.toEntity(user))
            .let(UserPersistenceMapper::toDomain)
    }
}
