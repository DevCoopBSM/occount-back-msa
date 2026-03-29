package devcoop.occount.point.application.usecase.deduct

import devcoop.occount.point.application.exception.PointNotFoundException
import devcoop.occount.point.application.output.PointRepository
import devcoop.occount.point.application.query.balance.PointBalanceResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeductPointUseCase(
    private val pointRepository: PointRepository,
) {
    @Transactional
    fun deduct(userId: Long, amount: Int): PointBalanceResponse {
        val point = pointRepository.findByUserId(userId) ?: throw PointNotFoundException()

        val savedPoint = pointRepository.save(point.deduct(amount))
        return PointBalanceResponse(balance = savedPoint.balance)
    }
}
