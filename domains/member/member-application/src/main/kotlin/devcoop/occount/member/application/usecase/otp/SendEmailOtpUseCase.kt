package devcoop.occount.member.application.usecase.otp

import devcoop.occount.member.application.exception.OtpRateLimitException
import devcoop.occount.member.application.otp.EmailOtp
import devcoop.occount.member.application.otp.OtpPurpose
import devcoop.occount.member.application.output.EmailOtpRepository
import devcoop.occount.member.application.output.EmailSender
import org.springframework.dao.DataIntegrityViolationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.security.SecureRandom
import java.time.Instant
import java.util.concurrent.CompletableFuture

@Service
class SendEmailOtpUseCase(
    private val emailOtpRepository: EmailOtpRepository,
    private val emailSender: EmailSender,
    transactionManager: PlatformTransactionManager,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val secureRandom = SecureRandom()
    private val transactionTemplate = TransactionTemplate(transactionManager)

    fun send(email: String, purpose: OtpPurpose = OtpPurpose.REGISTER) {
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
                        purpose = purpose,
                        createdAt = now,
                    )
                )
            } catch (_: DataIntegrityViolationException) {
                throw OtpRateLimitException()
            }

            code
        }!!

        CompletableFuture.runAsync {
            runCatching { emailSender.sendOtp(to = email, otpCode = otpCode) }
                .onSuccess { log.info("OTP 이메일 전송 성공 - email={}", email) }
                .onFailure { log.error("OTP 이메일 전송 실패 - email={}", email, it) }
        }
    }

    private fun generateOtpCode(): String =
        (secureRandom.nextInt(900_000) + 100_000).toString()
}
