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
    // 실패 처리(만료/잠금 시 삭제, 불일치 시 failCount 증가)는 예외를 던지면서도 반드시 커밋돼야 한다.
    // 기본 롤백 정책이면 이 write들이 롤백되어 OTP 잠금이 영구히 동작하지 않으므로 noRollbackFor로 제외한다.
    @Transactional(
        noRollbackFor = [
            OtpExpiredException::class,
            OtpLockedException::class,
            OtpMismatchException::class,
        ],
    )
    fun verify(email: String, otpCode: String) {
        val emailOtp = emailOtpRepository.findByEmailForUpdate(email)
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
