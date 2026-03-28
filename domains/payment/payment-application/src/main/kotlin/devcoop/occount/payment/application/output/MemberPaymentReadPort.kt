package devcoop.occount.payment.application.output

import devcoop.occount.payment.application.shared.PaymentUserInfo

interface MemberPaymentReadPort {
    fun getUser(userId: Long): PaymentUserInfo
}
