package devcoop.occount.member.infrastructure.mail

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty

@EnabledIfSystemProperty(named = "mail.oauth2.test", matches = "true")
class OAuthEmailSendTest {

    @Test
    fun `Gmail OAuth2로 OTP 메일 전송`() {
        val username = System.getProperty("mail.username")?.trim()
            ?: error("mail.username not set")
        val clientId = System.getProperty("mail.oauth2.client-id")?.trim()
            ?: error("mail.oauth2.client-id not set")
        val clientSecret = System.getProperty("mail.oauth2.client-secret")?.trim()
            ?: error("mail.oauth2.client-secret not set")
        val refreshToken = System.getProperty("mail.oauth2.refresh-token")?.trim()
            ?: error("mail.oauth2.refresh-token not set")

        val sender = OAuthJavaMailSender(username, clientId, clientSecret, refreshToken)
        val emailSender = EmailSenderImpl(sender, username)

        emailSender.sendOtp(to = "24.016@bssm.hs.kr", otpCode = "123456")

        println("메일 전송 성공!")
    }
}
