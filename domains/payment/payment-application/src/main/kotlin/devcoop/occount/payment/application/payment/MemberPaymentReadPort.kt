package devcoop.occount.payment.application.payment

interface MemberPaymentReadPort {
    fun getUser(userId: Long): PaymentUserInfo
}
