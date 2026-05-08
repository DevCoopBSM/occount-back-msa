package devcoop.occount.member.bootstrap.warmup

import devcoop.occount.member.application.output.TokenGenerator
import devcoop.occount.member.application.output.UserRepository
import devcoop.occount.member.application.usecase.login.KioskLoginRequest
import devcoop.occount.member.application.usecase.login.LoginUserUseCase
import devcoop.occount.member.application.usecase.login.MemberLoginRequest
import devcoop.occount.warmup.BusinessWarmup
import devcoop.occount.warmup.WarmupProbe
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class MemberBusinessWarmup(
    private val passwordEncoder: PasswordEncoder,
    private val tokenGenerator: TokenGenerator,
    private val userRepository: UserRepository,
    private val loginUserUseCase: LoginUserUseCase,
) : BusinessWarmup {

    override fun warmup() {
        val encoded = passwordEncoder.encode(WarmupProbe.PASSWORD)
        passwordEncoder.matches(WarmupProbe.PASSWORD, encoded)

        tokenGenerator.createAccessToken(WarmupProbe.USER_ID, WarmupProbe.ROLE)
        tokenGenerator.createKioskToken(WarmupProbe.USER_ID, WarmupProbe.ROLE)

        userRepository.findByEmail(WarmupProbe.EMAIL)
        userRepository.findByUserBarcode(WarmupProbe.BARCODE)
        userRepository.findById(WarmupProbe.USER_ID)

        runCatching {
            loginUserUseCase.login(MemberLoginRequest(WarmupProbe.EMAIL, WarmupProbe.PASSWORD))
        }
        runCatching {
            loginUserUseCase.login(KioskLoginRequest(WarmupProbe.BARCODE, WarmupProbe.PIN))
        }
    }
}
