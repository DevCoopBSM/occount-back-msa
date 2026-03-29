package devcoop.occount.point.application.query.balance

import devcoop.occount.point.application.exception.PointNotFound
import devcoop.occount.point.application.output.PointRepository
import org.springframework.stereotype.Service

@Service
class GetPointBalanceQueryService (
    private val pointRepository: PointRepository,
) {
    fun getBalance(userId: Long): PointBalanceResponse {
        val point = pointRepository.findByUserId(userId)
            ?: throw PointNotFound()
        return PointBalanceResponse(balance = point.balance)
    }
}
