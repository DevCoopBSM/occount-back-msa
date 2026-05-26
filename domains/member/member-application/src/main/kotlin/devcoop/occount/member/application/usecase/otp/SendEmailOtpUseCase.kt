package devcoop.occount.member.application.usecase.otp

import devcoop.occount.member.application.exception.OtpRateLimitException
import devcoop.occount.member.application.otp.EmailOtp
import devcoop.occount.member.application.output.EmailOtpRepository
import devcoop.occount.member.application.output.EmailSender
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.security.SecureRandom
import java.time.Instant

@Service
class SendEmailOtpUseCase(
    private val emailOtpRepository: EmailOtpRepository,
    private val emailSender: EmailSender,
    transactionManager: PlatformTransactionManager,
) {
    private val secureRandom = SecureRandom()
    private val transactionTemplate = TransactionTemplate(transactionManager)

    fun send(email: String) {
        val otpCode = transactionTemplate.execute {
            val existing = emailOtpRepository.findByEmailForUpdate(email)
            if (existing != null && existing.isRecentlySent()) {
                throw OtpRateLimitException()
            }

            val code = generateOtpCode()
            val now = Instant.now()

            try {
                emailOtpRepository.save(
                    EmailOtp(
                        email = email,
                        otpCode = code,
                        expiresAt = now.plusSeconds(EmailOtp.OTP_TTL_SECONDS),
                        createdAt = now,
                    )
                )
            } catch (_: DataIntegrityViolationException) {
                throw OtpRateLimitException()
            }

            code
        }!!

        emailSender.sendOtp(to = email, otpCode = otpCode)
    }

    private fun generateOtpCode(): String =
        (secureRandom.nextInt(900_000) + 100_000).toString()
}
