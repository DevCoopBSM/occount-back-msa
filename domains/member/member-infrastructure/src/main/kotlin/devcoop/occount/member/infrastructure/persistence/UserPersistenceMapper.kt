package devcoop.occount.member.infrastructure.persistence

import devcoop.occount.member.domain.user.User
import devcoop.occount.member.domain.user.AccountInfo
import devcoop.occount.member.domain.user.UserInfo
import devcoop.occount.member.domain.user.UserSensitiveInfo
import devcoop.occount.member.infrastructure.crypto.CryptoHelper
import java.time.LocalDate

object UserPersistenceMapper {
    fun toDomain(entity: UserJpaEntity, cryptoHelper: CryptoHelper): User {
        return User(
            id = entity.id,
            userInfo = UserInfo(
                username = entity.username,
                phone = cryptoHelper.decryptIfEncrypted(entity.phone),
                userBarcode = entity.userBarcode,
                userType = entity.userType,
                cooperativeNumber = entity.cooperativeNumber,
                birthDate = decryptBirthDate(entity.birthDate, cryptoHelper),
            ),
            accountInfo = AccountInfo(
                email = entity.email,
                password = entity.password,
                role = entity.role,
                pin = entity.pin,
            ),
            userSensitiveInfo = UserSensitiveInfo(
                ciNumber = cryptoHelper.decryptIfEncrypted(entity.userCiNumber),
            ),
        )
    }

    fun toEntity(
        domain: User,
        encryptedPhone: String?,
        encryptedCiNumber: String?,
        encryptedBirthDate: String?,
    ): UserJpaEntity {
        return UserJpaEntity(
            id = domain.getId(),
            username = domain.getUsername(),
            phone = encryptedPhone,
            userBarcode = domain.getUserBarcode(),
            userType = domain.getUserType(),
            cooperativeNumber = domain.getCooperativeNumber(),
            email = domain.getEmail(),
            password = domain.getPassword(),
            role = domain.getRole(),
            pin = domain.getUserPin(),
            userCiNumber = encryptedCiNumber,
            birthDate = encryptedBirthDate,
        )
    }

    private fun decryptBirthDate(value: String?, cryptoHelper: CryptoHelper): LocalDate? {
        return cryptoHelper.decryptIfEncrypted(value)
            ?.let(LocalDate::parse)
    }
}
