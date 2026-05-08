package devcoop.occount.member.application.output

interface EmailSender {
    fun sendOtp(to: String, otpCode: String)
}
