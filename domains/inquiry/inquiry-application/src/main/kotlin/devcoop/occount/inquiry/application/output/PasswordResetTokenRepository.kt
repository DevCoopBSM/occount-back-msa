package devcoop.occount.inquiry.application.output

import devcoop.occount.inquiry.domain.password.PasswordResetToken

interface PasswordResetTokenRepository {
    fun save(passwordResetToken: PasswordResetToken): PasswordResetToken
    fun findByToken(token: String): PasswordResetToken?
}
