package devcoop.occount.inquiry.application.usecase.password

import devcoop.occount.inquiry.application.output.EmailSender
import devcoop.occount.inquiry.application.output.PasswordResetTokenRepository
import devcoop.occount.inquiry.domain.password.PasswordResetToken
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SendPasswordResetEmailUseCase(
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val emailSender: EmailSender,
    @param:Value("\${app.password-reset.base-url}")
    private val passwordResetBaseUrl: String,
) {
    fun send(email: String) {
        val token = UUID.randomUUID().toString()

        passwordResetTokenRepository.save(
            PasswordResetToken(
                email = email,
                token = token,
            )
        )

        val resetLink = "$passwordResetBaseUrl/$token"
        emailSender.sendPasswordResetLink(email, resetLink)
    }
}
