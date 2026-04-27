package devcoop.occount.inquiry.application.usecase.email

import devcoop.occount.inquiry.application.output.EmailSender
import devcoop.occount.inquiry.application.output.EmailVerificationCodeRepository
import devcoop.occount.inquiry.domain.email.EmailVerificationCode
import org.springframework.stereotype.Service
import java.security.SecureRandom

@Service
class SendEmailVerificationCodeUseCase(
    private val emailVerificationCodeRepository: EmailVerificationCodeRepository,
    private val emailSender: EmailSender,
) {
    fun send(email: String) {
        val code = generateCode()

        emailVerificationCodeRepository.save(
            EmailVerificationCode(
                email = email,
                code = code,
            )
        )

        emailSender.sendVerificationCode(email, code)
    }

    private fun generateCode(): String {
        val random = SecureRandom()
        return String.format("%06d", random.nextInt(1_000_000))
    }
}
