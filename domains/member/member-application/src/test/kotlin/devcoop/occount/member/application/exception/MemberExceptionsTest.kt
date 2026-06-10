package devcoop.occount.member.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import kotlin.test.assertEquals

@DisplayName("member 예외 클래스 단위 테스트")
class MemberExceptionsTest {
    @Test
    @DisplayName("각 예외는 의도한 ErrorMessage를 가진 BusinessBaseException이다")
    fun `each exception carries its error message`() {
        val cases: List<Pair<BusinessBaseException, ErrorMessage>> = listOf(
            EmailNotVerifiedException() to ErrorMessage.EMAIL_NOT_VERIFIED,
            IdentityNotVerifiedException() to ErrorMessage.IDENTITY_NOT_VERIFIED,
            IdentityVerificationFailedException() to ErrorMessage.IDENTITY_VERIFICATION_FAILED,
            InvalidPasswordException() to ErrorMessage.INVALID_PASSWORD,
            InvalidPinException() to ErrorMessage.INVALID_PIN,
            OtpExpiredException() to ErrorMessage.OTP_EXPIRED,
            OtpLockedException() to ErrorMessage.OTP_LOCKED,
            OtpMismatchException() to ErrorMessage.OTP_MISMATCH,
            OtpNotFoundException() to ErrorMessage.OTP_NOT_FOUND,
            OtpRateLimitException() to ErrorMessage.OTP_RATE_LIMITED,
            UserAlreadyExistsException() to ErrorMessage.USER_ALREADY_EXISTS,
            UserNotFoundException() to ErrorMessage.USER_NOT_FOUND,
        )

        assertAll(
            cases.map { (exception, expected) ->
                { assertEquals(expected, exception.errorMessage) }
            },
        )
    }
}
