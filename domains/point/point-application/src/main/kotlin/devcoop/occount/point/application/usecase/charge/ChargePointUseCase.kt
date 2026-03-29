package devcoop.occount.point.application.usecase.charge

import devcoop.occount.point.application.exception.PointNotFoundException
import devcoop.occount.point.application.output.ChargePaymentPort
import devcoop.occount.point.application.output.PointRepository
import devcoop.occount.point.domain.ChargeLog
import devcoop.occount.point.domain.ChargeLogRepository
import devcoop.occount.point.domain.vo.PointTransaction
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChargePointUseCase(
    private val pointRepository: PointRepository,
    private val chargeLogRepository: ChargeLogRepository,
    private val chargePaymentPort: ChargePaymentPort,
) {
    @Transactional
    fun charge(request: ChargePointRequest) {
        val point = pointRepository.findByUserId(request.userId) ?: throw PointNotFoundException()

        val paymentId = chargePaymentPort.processCharge(
            userId = request.userId,
            amount = request.amount,
        )

        val beforeBalance = point.balance
        val updatedPoint = pointRepository.save(point.charge(request.amount))
        val afterBalance = updatedPoint.balance

        chargeLogRepository.save(
            ChargeLog(
                userId = request.userId,
                chargeAmount = request.amount,
                paymentId = paymentId,
                pointTransaction = PointTransaction(
                    beforePoint = beforeBalance,
                    afterPoint = afterBalance,
                ),
            )
        )
    }
}
