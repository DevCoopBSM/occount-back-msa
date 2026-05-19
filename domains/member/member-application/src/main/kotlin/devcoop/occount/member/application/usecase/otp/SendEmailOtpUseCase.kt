package devcoop.occount.member.application.usecase.otp

import devcoop.occount.member.application.exception.OtpRateLimitException
import devcoop.occount.member.application.otp.EmailOtp
import devcoop.occount.member.application.output.EmailOtpRepository
import devcoop.occount.member.application.output.EmailSender
import org.springframework.dao.DataIntegrityViolationException
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
        val existing = emailOtpRepository.findByEmailForUpdate(email)
        if (existing != null && existing.isRecentlySent()) {
            throw OtpRateLimitException()
        }

        val otpCode = generateOtpCode()
        val now = Instant.now()

        try {
            emailOtpRepository.save(
                EmailOtp(
                    email = email,
                    otpCode = otpCode,
                    expiresAt = now.plusSeconds(EmailOtp.OTP_TTL_SECONDS),
                    createdAt = now,
                )
            )
        } catch (_: DataIntegrityViolationException) {
            // 동시 요청이 먼저 저장한 경우 rate limit으로 처리
            throw OtpRateLimitException()
        }

        emailSender.sendOtp(to = email, otpCode = otpCode)
    }

    private fun generateOtpCode(): String =
        (secureRandom.nextInt(900_000) + 100_000).toString()
}
