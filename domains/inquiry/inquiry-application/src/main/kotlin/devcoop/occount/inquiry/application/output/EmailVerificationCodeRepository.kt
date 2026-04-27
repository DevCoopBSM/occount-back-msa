package devcoop.occount.inquiry.application.output

import devcoop.occount.inquiry.domain.email.EmailVerificationCode

interface EmailVerificationCodeRepository {
    fun save(emailVerificationCode: EmailVerificationCode): EmailVerificationCode
    fun findLatestByEmail(email: String): EmailVerificationCode?
}
