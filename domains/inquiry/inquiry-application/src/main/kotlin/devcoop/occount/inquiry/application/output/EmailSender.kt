package devcoop.occount.inquiry.application.output

interface EmailSender {
    fun sendVerificationCode(to: String, code: String)
    fun sendPasswordResetLink(to: String, resetLink: String)
}
