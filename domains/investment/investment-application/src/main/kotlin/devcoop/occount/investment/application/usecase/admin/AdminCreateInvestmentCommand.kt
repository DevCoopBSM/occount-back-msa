package devcoop.occount.investment.application.usecase.admin

import java.time.LocalDateTime

/**
 * @property type 출자 유형 이름(DEPOSIT/DONATION/RETURNED). 도메인 enum 으로는 유스케이스에서 변환한다.
 */
data class AdminCreateInvestmentCommand(
    val userId: Long,
    val amount: Int,
    val type: String,
    val depositDate: LocalDateTime,
    val conversionDate: LocalDateTime?,
    val confirmMethod: String,
)
