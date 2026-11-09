package devcoop.occount.member.application.usecase.login

import devcoop.occount.member.application.exception.InvalidPasswordException
import devcoop.occount.member.application.exception.InvalidPinException
import devcoop.occount.member.application.exception.KioskLoginLockedException
import devcoop.occount.member.application.exception.UserNotFoundException
import devcoop.occount.member.application.login.KioskLoginAttempt
import devcoop.occount.member.application.output.KioskLoginAttemptRepository
import devcoop.occount.member.application.output.TokenGenerator
import devcoop.occount.member.application.output.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class LoginUserUseCase(
    private val userRepository: UserRepository,
    private val tokenGenerator: TokenGenerator,
    private val passwordEncoder: PasswordEncoder,
    private val kioskLoginAttemptRepository: KioskLoginAttemptRepository,
) {
    fun login(request: MemberLoginRequest): String {
        val user = userRepository.findByEmail(request.email)
            ?: throw UserNotFoundException()

        if (!user.matchesPassword(request.password) { raw, encoded -> passwordEncoder.matches(raw, encoded) }) {
            throw InvalidPasswordException()
        }

        return tokenGenerator.createAccessToken(user.getId(), user.getRole().name)
    }

    // 트랜잭션을 두지 않는다(저장소 규칙): read 2 + write 1 이며 다중 write 원자성이 필요 없고,
    // 실패 카운트 save 직후 예외를 던지므로 트랜잭션이 있으면 롤백되어 카운트가 누적되지 않는다.
    fun login(request: KioskLoginRequest): String {
        val user = userRepository.findByUserBarcode(request.userBarcode)
            ?: throw UserNotFoundException()

        // 실존 barcode에 대해서만 시도 기록을 둔다(임의 barcode 탐색으로 인한 테이블 폭증 방지).
        val attempt = kioskLoginAttemptRepository.findByBarcode(request.userBarcode)
            ?: KioskLoginAttempt.initial(request.userBarcode)
        if (attempt.isLocked()) {
            throw KioskLoginLockedException()
        }

        if (!user.matchesPin(request.userPin) { raw, encoded -> passwordEncoder.matches(raw, encoded) }) {
            kioskLoginAttemptRepository.save(attempt.registerFailure())
            throw InvalidPinException()
        }

        kioskLoginAttemptRepository.deleteByBarcode(request.userBarcode)
        return tokenGenerator.createKioskToken(user.getId(), user.getRole().name)
    }
}
