package devcoop.occount.member.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class UserSensitiveInfoJpaEmbeddable(
    @field:Column(unique = true)
    private var userCiNumber: String? = null,
) {
    fun getUserCiNumber() = userCiNumber
}
