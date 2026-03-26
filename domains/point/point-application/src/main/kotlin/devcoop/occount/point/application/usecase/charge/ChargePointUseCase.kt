package devcoop.occount.point.application.usecase.charge

import devcoop.occount.point.application.exception.PointNotFound
import devcoop.occount.point.application.output.PointRepository
import devcoop.occount.point.application.query.balance.PointBalanceResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChargePointUseCase(
    private val pointRepository: PointRepository,
) {
    @Transactional
    fun charge(request: ChargePointRequest): PointBalanceResponse {
        val point = pointRepository.findByUserId(request.userId) ?: throw PointNotFound()

        val savedPoint = pointRepository.save(point.charge(request.amount))
        return PointBalanceResponse(balance = savedPoint.balance)
    }
}
