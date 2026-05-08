package devcoop.occount.member.application.usecase.otp

import devcoop.occount.member.application.exception.OtpExpiredException
import devcoop.occount.member.application.exception.OtpMismatchException
import devcoop.occount.member.application.exception.OtpNotFoundException
import devcoop.occount.member.application.output.EmailOtpRepository
import org.springframework.stereotype.Service

@Service
class VerifyEmailOtpUseCase(
    private val emailOtpRepository: EmailOtpRepository,
) {
    fun verify(email: String, otpCode: String) {
        val emailOtp = emailOtpRepository.findByEmail(email)
            ?: throw OtpNotFoundException()

        if (emailOtp.isExpired()) {
            emailOtpRepository.deleteByEmail(email)
            throw OtpExpiredException()
        }

        if (emailOtp.otpCode != otpCode) {
            throw OtpMismatchException()
        }

        emailOtpRepository.save(emailOtp.verify())
    }
}
