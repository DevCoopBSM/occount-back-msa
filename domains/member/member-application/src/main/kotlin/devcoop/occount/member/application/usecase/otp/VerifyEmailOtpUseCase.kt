package devcoop.occount.member.application.usecase.otp

import devcoop.occount.member.application.exception.OtpExpiredException
import devcoop.occount.member.application.exception.OtpLockedException
import devcoop.occount.member.application.exception.OtpMismatchException
import devcoop.occount.member.application.exception.OtpNotFoundException
import devcoop.occount.member.application.output.EmailOtpRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VerifyEmailOtpUseCase(
    private val emailOtpRepository: EmailOtpRepository,
) {
    @Transactional
    fun verify(email: String, otpCode: String) {
        val emailOtp = emailOtpRepository.findByEmail(email)
            ?: throw OtpNotFoundException()

        if (emailOtp.isExpired()) {
            emailOtpRepository.deleteByEmail(email)
            throw OtpExpiredException()
        }

        if (emailOtp.isLocked()) {
            emailOtpRepository.deleteByEmail(email)
            throw OtpLockedException()
        }

        if (emailOtp.otpCode != otpCode) {
            emailOtpRepository.save(emailOtp.incrementFailCount())
            throw OtpMismatchException()
        }

        emailOtpRepository.save(emailOtp.verify())
    }
}
