package devcoop.occount.member.bootstrap.warmup

import devcoop.occount.member.application.output.TokenGenerator
import devcoop.occount.member.application.output.UserRepository
import devcoop.occount.member.application.usecase.login.KioskLoginRequest
import devcoop.occount.member.application.usecase.login.LoginUserUseCase
import devcoop.occount.member.application.usecase.login.MemberLoginRequest
import devcoop.occount.warmup.WarmupProbe
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.crypto.password.PasswordEncoder

class MemberBusinessWarmupTest {
    @Test
    fun `warmup exercises password encoder, token generator, repository and login use case`() {
        val passwordEncoder = mock(PasswordEncoder::class.java)
        val tokenGenerator = mock(TokenGenerator::class.java)
        val userRepository = mock(UserRepository::class.java)
        val loginUserUseCase = mock(LoginUserUseCase::class.java)

        `when`(passwordEncoder.encode(anyString())).thenReturn("\$2a\$10\$dummyBcryptHashForWarmupTest")
        `when`(passwordEncoder.matches(anyString(), anyString())).thenReturn(false)
        `when`(tokenGenerator.createAccessToken(anyLong(), anyString())).thenReturn("token")
        `when`(tokenGenerator.createKioskToken(anyLong(), anyString())).thenReturn("token")

        MemberBusinessWarmup(passwordEncoder, tokenGenerator, userRepository, loginUserUseCase).warmup()

        verify(passwordEncoder).encode(WarmupProbe.PASSWORD)
        verify(passwordEncoder).matches(anyString(), anyString())
        verify(tokenGenerator).createAccessToken(WarmupProbe.USER_ID, WarmupProbe.ROLE)
        verify(tokenGenerator).createKioskToken(WarmupProbe.USER_ID, WarmupProbe.ROLE)
        verify(userRepository).findByEmail(WarmupProbe.EMAIL)
        verify(userRepository).findByUserBarcode(WarmupProbe.BARCODE)
        verify(userRepository).findById(WarmupProbe.USER_ID)
    }

    @Test
    fun `warmup swallows exceptions thrown by login use case`() {
        val passwordEncoder = mock(PasswordEncoder::class.java)
        val tokenGenerator = mock(TokenGenerator::class.java)
        val userRepository = mock(UserRepository::class.java)
        val loginUserUseCase = mock(LoginUserUseCase::class.java)

        `when`(passwordEncoder.encode(anyString())).thenReturn("\$2a\$10\$dummyBcryptHashForWarmupTest")
        `when`(loginUserUseCase.login(anyMemberLogin()))
            .thenThrow(RuntimeException("probe member login failed"))
        `when`(loginUserUseCase.login(anyKioskLogin()))
            .thenThrow(RuntimeException("probe kiosk login failed"))

        // 로그인에서 예외가 발생해도 runCatching이 삼켜 warmup이 정상 종료된다
        MemberBusinessWarmup(passwordEncoder, tokenGenerator, userRepository, loginUserUseCase).warmup()

        verify(loginUserUseCase).login(anyMemberLogin())
        verify(loginUserUseCase).login(anyKioskLogin())
    }

    // Kotlin non-null 파라미터에 안전하게 쓸 수 있는 any() 매처
    private fun anyMemberLogin(): MemberLoginRequest {
        any(MemberLoginRequest::class.java)
        return MemberLoginRequest("", "")
    }

    private fun anyKioskLogin(): KioskLoginRequest {
        any(KioskLoginRequest::class.java)
        return KioskLoginRequest("", "")
    }
}
