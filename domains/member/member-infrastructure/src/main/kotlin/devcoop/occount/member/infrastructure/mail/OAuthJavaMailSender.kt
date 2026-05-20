package devcoop.occount.member.infrastructure.mail

import com.google.auth.oauth2.UserCredentials
import jakarta.mail.internet.MimeMessage
import org.springframework.mail.javamail.JavaMailSenderImpl

class OAuthJavaMailSender(
    private val email: String,
    clientId: String,
    clientSecret: String,
    refreshToken: String,
) : JavaMailSenderImpl() {

    private val credential: UserCredentials = UserCredentials.newBuilder()
        .setClientId(clientId)
        .setClientSecret(clientSecret)
        .setRefreshToken(refreshToken)
        .build()

    init {
        host = "smtp.gmail.com"
        port = 587
        username = email
        val props = javaMailProperties
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.auth.mechanisms"] = "XOAUTH2"
        props["mail.smtp.starttls.enable"] = "true"
        props["mail.smtp.ssl.protocols"] = "TLSv1.2"
        props["mail.smtp.ssl.trust"] = "smtp.gmail.com"
    }

    override fun doSend(mimeMessages: Array<out MimeMessage>, originalMessages: Array<out Any>?) {
        credential.refresh()
        password = credential.accessToken.tokenValue
        super.doSend(mimeMessages, originalMessages)
    }
}
