package devcoop.occount.member.infrastructure.mail

import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import java.util.Properties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.mail.javamail.JavaMailSender

@DisplayName("EmailSenderImpl 단위 테스트")
class EmailSenderImplTest {
    @Test
    @DisplayName("sendOtp는 발신자/수신자/제목/HTML 본문을 구성해 메일을 전송한다")
    fun `sendOtp builds and sends the message`() {
        val javaMailSender = mock(JavaMailSender::class.java)
        val mimeMessage = MimeMessage(Session.getInstance(Properties()))
        `when`(javaMailSender.createMimeMessage()).thenReturn(mimeMessage)

        val sender = EmailSenderImpl(javaMailSender, "from@occount.com")
        sender.sendOtp(to = "to@test.com", otpCode = "123456")

        val captor = ArgumentCaptor.forClass(MimeMessage::class.java)
        verify(javaMailSender).send(captor.capture())

        val sent = captor.value
        assertEquals("[Occount] 이메일 인증번호", sent.subject)
        assertEquals("to@test.com", sent.getRecipients(jakarta.mail.Message.RecipientType.TO).single().toString())
        assertEquals("from@occount.com", sent.from.single().toString())
    }
}
