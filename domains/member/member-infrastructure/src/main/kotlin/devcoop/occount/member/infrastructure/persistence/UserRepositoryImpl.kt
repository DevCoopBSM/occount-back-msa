package devcoop.occount.member.infrastructure.persistence

import devcoop.occount.member.domain.user.User
import devcoop.occount.member.application.output.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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

    override fun findAll(pageable: Pageable): Page<User> {
        return userJpaRepository.findAll(pageable)
            .map(UserPersistenceMapper::toDomain)
    }

    override fun searchByKeyword(keyword: String, pageable: Pageable): Page<User> {
        return userJpaRepository.searchByKeyword(escapeLikePattern(keyword), pageable)
            .map(UserPersistenceMapper::toDomain)
    }

    // LIKE 메타문자를 이스케이프해 keyword가 리터럴 부분일치로만 매칭되도록 한다(ESCAPE '\' 와 짝).
    private fun escapeLikePattern(keyword: String): String =
        keyword.replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
}
