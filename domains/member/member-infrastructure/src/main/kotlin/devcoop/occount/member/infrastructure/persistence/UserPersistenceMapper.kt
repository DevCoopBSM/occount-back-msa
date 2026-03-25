package devcoop.occount.member.infrastructure.persistence

import devcoop.occount.member.domain.user.User
import devcoop.occount.member.domain.user.AccountInfo
import devcoop.occount.member.domain.user.UserInfo
import devcoop.occount.member.domain.user.UserSensitiveInfo

object UserPersistenceMapper {
    fun toDomain(entity: UserJpaEntity): User {
        val userInfoEntity = entity.getUserInfo()
        val userAccountInfoEntity = entity.getUserAccountInfo()
        val userSensitiveInfoEntity = entity.getUserSensitiveInfo()

        return User(
            id = entity.getId(),
            userInfo = UserInfo(
                username = userInfoEntity.getUsername(),
                phone = userInfoEntity.getPhone(),
                userBarcode = userInfoEntity.getUserBarcode(),
                userType = userInfoEntity.getUserType(),
                cooperativeNumber = userInfoEntity.getCooperativeNumber(),
            ),
            accountInfo = AccountInfo(
                email = userAccountInfoEntity.getUserEmail(),
                password = userAccountInfoEntity.getUserPassword(),
                role = userAccountInfoEntity.getRole(),
                pin = userAccountInfoEntity.getPin(),
            ),
            userSensitiveInfo = UserSensitiveInfo(
                ciNumber = userSensitiveInfoEntity.getUserCiNumber(),
            ),
        )
    }

    fun toEntity(domain: User): UserJpaEntity {
        return UserJpaEntity(
            id = domain.getId(),
            userInfo = UserInfoJpaEmbeddable(
                username = domain.getUsername(),
                phone = domain.getPhone(),
                userBarcode = domain.getUserBarcode(),
                userType = domain.getUserType(),
                cooperativeNumber = domain.getCooperativeNumber(),
            ),
            userAccountInfo = UserAccountInfoJpaEmbeddable(
                userEmail = domain.getEmail(),
                userPassword = domain.getPassword(),
                role = domain.getRole(),
                pin = domain.getUserPin(),
            ),
            userSensitiveInfo = UserSensitiveInfoJpaEmbeddable(
                userCiNumber = domain.getCiNumber(),
            ),
        )
    }
}
