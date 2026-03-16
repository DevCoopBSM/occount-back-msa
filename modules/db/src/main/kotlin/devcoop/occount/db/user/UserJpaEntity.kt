package devcoop.occount.db.user

import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "common_user")
class UserJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long = 0L,
    @Embedded
    private var userInfo: UserInfoJpaEmbeddable = UserInfoJpaEmbeddable(),
    @Embedded
    private var userAccountInfo: UserAccountInfoJpaEmbeddable = UserAccountInfoJpaEmbeddable(),
    @Embedded
    private var userSensitiveInfo: UserSensitiveInfoJpaEmbeddable = UserSensitiveInfoJpaEmbeddable(),
) {
    fun getId() = id
    fun getUserInfo() = userInfo
    fun getUserAccountInfo() = userAccountInfo
    fun getUserSensitiveInfo() = userSensitiveInfo
}
