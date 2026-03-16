package devcoop.occount.db.user

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class UserSensitiveInfoJpaEmbeddable(
    @field:Column(unique = true)
    private var userCiNumber: String? = null,
    @field:Column(unique = true)
    private var userFingerPrint: String? = null,
) {
    fun getUserCiNumber() = userCiNumber
    fun getUserFingerPrint() = userFingerPrint
}
