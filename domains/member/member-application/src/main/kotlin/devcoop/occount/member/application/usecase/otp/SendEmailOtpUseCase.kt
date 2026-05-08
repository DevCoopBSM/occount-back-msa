package devcoop.occount.member.application.usecase.otp

import devcoop.occount.member.application.otp.EmailOtp
import devcoop.occount.member.application.output.EmailOtpRepository
import devcoop.occount.member.application.output.EmailSender
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import kotlin.random.Random

@Service
class SendEmailOtpUseCase(
    private val emailOtpRepository: EmailOtpRepository,
    private val emailSender: EmailSender,
) {
    fun send(email: String) {
        val otpCode = generateOtpCode()

        emailOtpRepository.save(
            EmailOtp(
                email = email,
                otpCode = otpCode,
                expiresAt = LocalDateTime.now().plusMinutes(5),
                verified = false,
            )
        )

        emailSender.sendOtp(to = email, otpCode = otpCode)
    }

    private fun generateOtpCode(): String =
        Random.nextInt(100_000, 1_000_000).toString()
}
