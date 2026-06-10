package devcoop.occount.member.infrastructure.mail

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("GmailOAuth2MailConfig 단위 테스트")
class GmailOAuth2MailConfigTest {
    @Test
    @DisplayName("javaMailSender 빈은 OAuthJavaMailSender를 생성한다")
    fun `javaMailSender bean creates an OAuthJavaMailSender`() {
        val config = GmailOAuth2MailConfig(
            username = "from@occount.com",
            clientId = "client-id",
            clientSecret = "client-secret",
            refreshToken = "refresh-token",
        )

        val sender = config.javaMailSender()

        assertTrue(sender is OAuthJavaMailSender)
    }
}
