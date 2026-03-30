package devcoop.occount.member.infrastructure.persistence

import devcoop.occount.member.domain.user.User
import devcoop.occount.member.domain.user.AccountInfo
import devcoop.occount.member.domain.user.UserInfo
import devcoop.occount.member.domain.user.UserSensitiveInfo

object UserPersistenceMapper {
    fun toDomain(entity: UserJpaEntity): User {
        return User(
            id = entity.id,
            userInfo = UserInfo(
                username = entity.username,
                phone = entity.phone,
                userBarcode = entity.userBarcode,
                userType = entity.userType,
                cooperativeNumber = entity.cooperativeNumber,
            ),
            accountInfo = AccountInfo(
                email = entity.email,
                password = entity.password,
                role = entity.role,
                pin = entity.pin,
            ),
            userSensitiveInfo = UserSensitiveInfo(
                ciNumber = entity.userCiNumber,
            ),
        )
    }

    fun toEntity(domain: User): UserJpaEntity {
        return UserJpaEntity(
            id = domain.getId(),
            username = domain.getUsername(),
            phone = domain.getPhone(),
            userBarcode = domain.getUserBarcode(),
            userType = domain.getUserType(),
            cooperativeNumber = domain.getCooperativeNumber(),
            email = domain.getEmail(),
            password = domain.getPassword(),
            role = domain.getRole(),
            pin = domain.getUserPin(),
            userCiNumber = domain.getCiNumber(),
        )
    }
}
