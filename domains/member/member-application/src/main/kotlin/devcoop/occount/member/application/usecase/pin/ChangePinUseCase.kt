package devcoop.occount.member.application.usecase.pin

import devcoop.occount.member.application.exception.InvalidPasswordException
import devcoop.occount.member.application.exception.UserNotFoundException
import devcoop.occount.member.application.output.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChangePinUseCase(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
    fun changePin(userId: Long, request: ChangePinRequest) {
        val user = userRepository.findById(userId)
            ?: throw UserNotFoundException()

        // 현재 비밀번호 확인으로 본인 인증을 대체한다.
        val matches = user.matchesPassword(request.password) { raw, encoded ->
            passwordEncoder.matches(raw, encoded)
        }
        if (!matches) {
            throw InvalidPasswordException()
        }

        userRepository.save(user.changePin(passwordEncoder.encode(request.newPin)!!))
    }
}
