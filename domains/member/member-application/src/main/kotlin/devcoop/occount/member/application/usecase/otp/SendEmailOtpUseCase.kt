package devcoop.occount.member.application.usecase.otp

import devcoop.occount.member.application.exception.OtpRateLimitException
import devcoop.occount.member.application.otp.EmailOtp
import devcoop.occount.member.application.output.EmailOtpRepository
import devcoop.occount.member.application.output.EmailSender
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Instant

@Service
class SendEmailOtpUseCase(
    private val emailOtpRepository: EmailOtpRepository,
    private val emailSender: EmailSender,
) {
    private val secureRandom = SecureRandom()

    @Transactional
    fun send(email: String) {
        val existing = emailOtpRepository.findByEmail(email)
        if (existing != null && existing.isRecentlySent()) {
            throw OtpRateLimitException()
        }

        val otpCode = generateOtpCode()

        emailOtpRepository.save(
            EmailOtp(
                email = email,
                otpCode = otpCode,
                expiresAt = Instant.now().plusSeconds(EmailOtp.OTP_TTL_SECONDS),
            )
        )

        emailSender.sendOtp(to = email, otpCode = otpCode)
    }

    private fun generateOtpCode(): String =
        (secureRandom.nextInt(900_000) + 100_000).toString()
}
