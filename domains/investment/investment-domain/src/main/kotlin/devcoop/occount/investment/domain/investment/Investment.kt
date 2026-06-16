package devcoop.occount.investment.domain.investment

import java.time.LocalDateTime

/**
 * 조합원 출자(출자금/기부금) 한 건.
 *
 * 결제 흐름: 인증 사용자가 출자를 시작하면 [pending] 으로 생성되고(paymentId 발급),
 * PortOne 결제 확정 웹훅이 도착하면 [confirm] 으로 CONFIRMED 가 된다.
 * 관리자 수기 등록은 [confirmedByAdmin] 으로 곧바로 CONFIRMED 로 생성한다.
 *
 * paymentId 는 결제 1건과 출자 1건을 잇는 멱등 키이며 유니크하다.
 */
data class Investment(
    val investmentId: Long = 0L,
    val userId: Long,
    val paymentId: String,
    val amount: Int,
    val type: InvestmentType = InvestmentType.DEPOSIT,
    val status: InvestmentStatus = InvestmentStatus.PENDING,
    val depositDate: LocalDateTime? = null,
    val conversionDate: LocalDateTime? = null,
    val confirmMethod: String = AUTO_CONFIRM_METHOD,
) {
    init {
        if (amount <= 0) {
            throw InvalidInvestmentAmountException()
        }
    }

    /**
     * 결제 확정. 이미 확정된 출자에 재호출하면 예외를 던진다(호출 측에서 멱등 처리할 것).
     */
    fun confirm(confirmedAt: LocalDateTime = LocalDateTime.now()): Investment {
        if (status == InvestmentStatus.CONFIRMED) {
            throw InvestmentAlreadyConfirmedException()
        }
        return copy(status = InvestmentStatus.CONFIRMED, depositDate = confirmedAt)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Investment) return false
        return investmentId == other.investmentId
    }

    override fun hashCode(): Int = investmentId.hashCode()

    companion object {
        const val AUTO_CONFIRM_METHOD = "포트원 자동확인"
        const val MANUAL_CONFIRM_METHOD = "수기확인"

        /**
         * PortOne 결제 시작 시점의 대기 출자.
         */
        fun pending(userId: Long, paymentId: String, amount: Int): Investment {
            return Investment(
                userId = userId,
                paymentId = paymentId,
                amount = amount,
                type = InvestmentType.DEPOSIT,
                status = InvestmentStatus.PENDING,
                confirmMethod = AUTO_CONFIRM_METHOD,
            )
        }

        /**
         * 관리자 수기 등록(즉시 확정).
         */
        fun confirmedByAdmin(
            userId: Long,
            paymentId: String,
            amount: Int,
            type: InvestmentType,
            depositDate: LocalDateTime,
            conversionDate: LocalDateTime?,
            confirmMethod: String,
        ): Investment {
            return Investment(
                userId = userId,
                paymentId = paymentId,
                amount = amount,
                type = type,
                status = InvestmentStatus.CONFIRMED,
                depositDate = depositDate,
                conversionDate = conversionDate,
                confirmMethod = confirmMethod,
            )
        }
    }
}
