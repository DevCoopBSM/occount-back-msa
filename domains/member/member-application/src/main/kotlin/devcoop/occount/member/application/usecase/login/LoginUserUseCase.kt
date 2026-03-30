package devcoop.occount.member.application.usecase.login

import devcoop.occount.member.application.exception.InvalidPasswordException
import devcoop.occount.member.application.exception.UserNotFoundException
import devcoop.occount.member.application.output.TokenGenerator
import devcoop.occount.member.application.output.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class LoginUserUseCase(
    private val userRepository: UserRepository,
    private val tokenGenerator: TokenGenerator,
    private val passwordEncoder: PasswordEncoder,
) {
    fun login(request: MemberLoginRequest): String {
        val user = userRepository.findByUserEmail(request.email)
            ?: throw UserNotFoundException()

        passwordValidate(request.password, user.getPassword())

        return tokenGenerator.createAccessToken(user.getId(), user.getRole().name)
    }

    fun login(request: KioskLoginRequest): String {
        val user = userRepository.findByUserBarcode(request.userBarcode)
            ?: throw UserNotFoundException()

        passwordValidate(request.userPin, user.getUserPin())

        return tokenGenerator.createKioskToken(user.getId(), user.getRole().name)
    }

    fun passwordValidate(requestPassword: String, expectedPassword: String) {
        if(!passwordEncoder.matches(requestPassword, expectedPassword)) {
            throw InvalidPasswordException()
        }
    }
}
