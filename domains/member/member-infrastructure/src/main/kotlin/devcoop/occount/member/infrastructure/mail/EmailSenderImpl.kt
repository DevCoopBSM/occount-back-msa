package devcoop.occount.member.infrastructure.mail

import devcoop.occount.member.application.output.EmailSender
import jakarta.mail.internet.MimeMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

@Component
class EmailSenderImpl(
    private val javaMailSender: JavaMailSender,
) : EmailSender {

    override fun sendOtp(to: String, otpCode: String) {
        val message: MimeMessage = javaMailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, false, "UTF-8")

        helper.setFrom("serverdvcp@gmail.com")
        helper.setTo(to)
        helper.setSubject("[Occount] 이메일 인증번호")
        helper.setText(buildHtml(otpCode), true)

        javaMailSender.send(message)
    }

    private fun buildHtml(otpCode: String): String = """
        <!DOCTYPE html>
        <html lang="ko">
        <head><meta charset="UTF-8"></head>
        <body style="font-family: Arial, sans-serif; color: #333; max-width: 480px; margin: 0 auto; padding: 24px;">
          <div style="background-color: #f9f9f9; padding: 32px; border-radius: 8px; text-align: center;">
            <h2 style="color: #4a4a4a;">Occount 이메일 인증</h2>
            <p>아래 인증번호를 앱에 입력해주세요.</p>
            <div style="font-size: 36px; font-weight: bold; letter-spacing: 8px; color: #F0CE00; margin: 24px 0;">
              $otpCode
            </div>
            <p style="font-size: 13px; color: #888;">이 인증번호는 <strong>5분</strong> 동안 유효합니다.</p>
            <p style="font-size: 13px; color: #888;">본인이 요청하지 않은 경우 이 메일을 무시해주세요.</p>
          </div>
        </body>
        </html>
    """.trimIndent()
}
