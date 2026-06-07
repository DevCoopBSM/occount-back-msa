package devcoop.occount.member.infrastructure.mail

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender

@Configuration
class GmailOAuth2MailConfig(
    @param:Value("\${spring.mail.username}") private val username: String,
    @param:Value("\${mail.oauth2.client-id}") private val clientId: String,
    @param:Value("\${mail.oauth2.client-secret}") private val clientSecret: String,
    @param:Value("\${mail.oauth2.refresh-token}") private val refreshToken: String,
) {
    @Bean
    fun javaMailSender(): JavaMailSender =
        OAuthJavaMailSender(username, clientId, clientSecret, refreshToken)
}
