package devcoop.occount.member.infrastructure.persistence

import devcoop.occount.member.domain.user.User
import devcoop.occount.member.application.output.UserRepository
import devcoop.occount.member.infrastructure.crypto.CryptoHelper
import devcoop.occount.member.infrastructure.crypto.UniqueEncryptedValueGenerator
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryImpl(
    private val userJpaRepository: UserJpaRepository,
    private val cryptoHelper: CryptoHelper,
) : UserRepository {
    override fun findById(id: Long): User? {
        return userJpaRepository.findById(id)
            .map { UserPersistenceMapper.toDomain(it, cryptoHelper) }
            .orElse(null)
    }

    override fun findByUserBarcode(userBarcode: String): User? {
        return userJpaRepository.findByUserBarcode(userBarcode)
            ?.let { UserPersistenceMapper.toDomain(it, cryptoHelper) }
    }

    override fun findByEmail(userEmail: String): User? {
        return userJpaRepository.findByEmail(userEmail)
            ?.let { UserPersistenceMapper.toDomain(it, cryptoHelper) }
    }

    override fun existsByEmail(userEmail: String): Boolean {
        return userJpaRepository.existsByEmail(userEmail)
    }

    override fun save(user: User): User {
        val encryptedPhone = encryptedValueGenerator.generate(user.getPhone(), userJpaRepository::existsByPhone)
        val encryptedCiNumber = encryptedValueGenerator.generate(user.getCiNumber(), userJpaRepository::existsByUserCiNumber)

        return userJpaRepository.saveAndFlush(
            UserPersistenceMapper.toEntity(
                domain = user,
                encryptedPhone = encryptedPhone,
                encryptedCiNumber = encryptedCiNumber,
            ),
        ).let { UserPersistenceMapper.toDomain(it, cryptoHelper) }
    }

    override fun findAll(pageable: Pageable): Page<User> {
        return userJpaRepository.findAll(pageable)
            .map { UserPersistenceMapper.toDomain(it, cryptoHelper) }
    }

    override fun searchByKeyword(keyword: String, pageable: Pageable): Page<User> {
        return userJpaRepository.searchByKeyword(escapeLikePattern(keyword), pageable)
            .map { UserPersistenceMapper.toDomain(it, cryptoHelper) }
    }

    // LIKE 메타문자를 이스케이프해 keyword가 리터럴 부분일치로만 매칭되도록 한다(ESCAPE '\' 와 짝).
    private fun escapeLikePattern(keyword: String): String =
        keyword.replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")

    private val encryptedValueGenerator = UniqueEncryptedValueGenerator(cryptoHelper)
}
