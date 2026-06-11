package devcoop.occount.member.application.usecase.password

import devcoop.occount.member.application.exception.OtpExpiredException
import devcoop.occount.member.application.exception.OtpLockedException
import devcoop.occount.member.application.exception.OtpMismatchException
import devcoop.occount.member.application.exception.OtpNotFoundException
import devcoop.occount.member.application.otp.OtpPurpose
import devcoop.occount.member.application.output.EmailOtpRepository
import devcoop.occount.member.application.output.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChangePasswordUseCase(
    private val userRepository: UserRepository,
    private val emailOtpRepository: EmailOtpRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // 성공 경로(비밀번호 저장 + OTP 삭제)는 원자성이 필요해 트랜잭션을 유지한다.
    // 단, 실패 처리(만료/잠금 시 삭제, 불일치 시 failCount 증가)는 예외를 던지면서도 커밋돼야 하므로
    // 기본 롤백에서 제외한다(제외하지 않으면 OTP 잠금이 동작하지 않는다).
    @Transactional(
        noRollbackFor = [
            OtpExpiredException::class,
            OtpLockedException::class,
            OtpMismatchException::class,
        ],
    )
    fun changePassword(request: ChangePasswordRequest) {
        // 1) 비밀번호 변경용으로 발송된 OTP 코드를 변경 요청 시점에 직접 재검증한다.
        //    (verified 플래그만 신뢰하지 않고 코드를 다시 제시받아, 코드 없이 변경되는 것을 차단)
        val emailOtp = emailOtpRepository.findByEmailForUpdate(request.email)
            ?.takeIf { it.purpose == OtpPurpose.PASSWORD_RESET }
            ?: throw OtpNotFoundException()

        if (emailOtp.isExpired()) {
            emailOtpRepository.deleteByEmail(request.email)
            throw OtpExpiredException()
        }

        if (emailOtp.isLocked()) {
            emailOtpRepository.deleteByEmail(request.email)
            throw OtpLockedException()
        }

        if (emailOtp.otpCode != request.otpCode) {
            emailOtpRepository.save(emailOtp.incrementFailCount())
            throw OtpMismatchException()
        }

        // 2) OTP 검증 통과. 유저 존재 여부와 무관하게 OTP를 소비하고 동일한 응답(204)을 반환해
        //    가입 여부가 노출되지 않도록 한다(email enumeration 방지).
        val user = userRepository.findByEmail(request.email)
        if (user != null) {
            userRepository.save(user.changePassword(passwordEncoder.encode(request.newPassword)!!))
        } else {
            log.warn("비밀번호 변경 요청: 인증은 통과했으나 가입된 유저 없음 - email={}", request.email)
        }

        emailOtpRepository.deleteByEmail(request.email)
    }
}
