package devcoop.occount.member.application.usecase.otp

import devcoop.occount.member.application.exception.OtpRateLimitException
import devcoop.occount.member.application.otp.EmailOtp
import devcoop.occount.member.application.output.EmailOtpRepository
import devcoop.occount.member.application.output.EmailSender
import devcoop.occount.member.application.support.FakeEmailOtpRepository
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionStatus

private val noopTransactionManager = object : AbstractPlatformTransactionManager() {
    override fun doGetTransaction(): Any = Any()
    override fun doBegin(transaction: Any, definition: TransactionDefinition) = Unit
    override fun doCommit(status: DefaultTransactionStatus) = Unit
    override fun doRollback(status: DefaultTransactionStatus) = Unit
}

private class RecordingEmailSender(
    private val shouldThrow: Boolean = false,
) : EmailSender {
    val latch = CountDownLatch(1)
    var sentTo: String? = null

    override fun sendOtp(to: String, otpCode: String) {
        sentTo = to
        latch.countDown()
        if (shouldThrow) {
            throw RuntimeException("smtp down")
        }
    }
}

@DisplayName("SendEmailOtpUseCase 단위 테스트")
class SendEmailOtpUseCaseTest {
    private val email = "test@test.com"

    @Test
    @DisplayName("OTP를 저장하고 비동기로 이메일을 전송한다")
    fun `saves otp and sends email asynchronously`() {
        val repository = FakeEmailOtpRepository()
        val sender = RecordingEmailSender()
        val useCase = SendEmailOtpUseCase(repository, sender, noopTransactionManager)

        useCase.send(email)

        assertNotNull(repository.findByEmail(email))
        assertTrue(sender.latch.await(2, TimeUnit.SECONDS))
        assertTrue(sender.sentTo == email)
    }

    @Test
    @DisplayName("이메일 전송이 실패해도 예외가 전파되지 않는다")
    fun `does not propagate exception when email sending fails`() {
        val repository = FakeEmailOtpRepository()
        val sender = RecordingEmailSender(shouldThrow = true)
        val useCase = SendEmailOtpUseCase(repository, sender, noopTransactionManager)

        useCase.send(email)

        assertNotNull(repository.findByEmail(email))
        assertTrue(sender.latch.await(2, TimeUnit.SECONDS))
        // onFailure 로깅 람다가 실행될 시간을 잠시 확보
        Thread.sleep(100)
    }

    @Test
    @DisplayName("재발송 쿨다운 이내에 다시 요청하면 OtpRateLimitException을 던진다")
    fun `throws OtpRateLimitException within resend cooldown`() {
        val recentOtp = EmailOtp(
            email = email,
            otpCode = "123456",
            expiresAt = Instant.now().plusSeconds(300),
            createdAt = Instant.now(),
        )
        val repository = FakeEmailOtpRepository(mapOf(email to recentOtp))
        val useCase = SendEmailOtpUseCase(repository, RecordingEmailSender(), noopTransactionManager)

        assertFailsWith<OtpRateLimitException> { useCase.send(email) }
    }

    @Test
    @DisplayName("저장 시 중복 제약 위반이 발생하면 OtpRateLimitException으로 변환한다")
    fun `converts DataIntegrityViolation to OtpRateLimitException`() {
        val repository = object : EmailOtpRepository by FakeEmailOtpRepository() {
            override fun findByEmailForUpdate(email: String): EmailOtp? = null
            override fun save(emailOtp: EmailOtp): EmailOtp = throw DataIntegrityViolationException("dup")
        }
        val useCase = SendEmailOtpUseCase(repository, RecordingEmailSender(), noopTransactionManager)

        assertFailsWith<OtpRateLimitException> { useCase.send(email) }
    }
}
