package devcoop.occount.member.application.output

import devcoop.occount.member.application.otp.EmailOtp

interface EmailOtpRepository {
    fun save(emailOtp: EmailOtp): EmailOtp
    fun findByEmail(email: String): EmailOtp?
    fun findValidByEmail(email: String): EmailOtp?
    fun deleteByEmail(email: String)
}
