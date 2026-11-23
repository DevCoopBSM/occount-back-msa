package devcoop.occount.order.application.support

import devcoop.occount.order.application.exception.OrderAccessDeniedException
import devcoop.occount.order.domain.order.OrderAggregate

internal object OrderAccessValidator {
    fun validate(order: OrderAggregate, userId: Long?, guestOrderToken: String?) {
        val ownerId = order.userId
        val allowed = if (ownerId != null) {
            ownerId == userId
        } else {
            val tokenHash = order.guestOrderTokenHash
            tokenHash != null && guestOrderToken != null && GuestOrderToken.matches(guestOrderToken, tokenHash)
        }
        if (!allowed) throw OrderAccessDeniedException()
    }
}
