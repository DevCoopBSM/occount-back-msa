package devcoop.occount.point.application.point

import devcoop.occount.point.domain.Point
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PointService(
    private val pointRepository: PointRepository,
) {
    @Transactional
    fun initialize(userId: Long) {
        try {
            pointRepository.save(Point(userId))
        } catch (_: DuplicateKeyException) {
            throw PointAlreadyInitializedException()
        }
    }

    fun getBalance(userId: Long): PointBalanceResponse {
        val point = pointRepository.findByUserId(userId)
            ?: throw PointNotFound()
        return PointBalanceResponse(balance = point.balance)
    }

    @Transactional
    fun charge(userId: Long, amount: Int): PointBalanceResponse {
        val savedPoint = pointRepository.save(findPoint(userId).charge(amount))
        return PointBalanceResponse(balance = savedPoint.balance)
    }

    @Transactional
    fun deduct(userId: Long, amount: Int): PointBalanceResponse {
        val savedPoint = pointRepository.save(findPoint(userId).deduct(amount))
        return PointBalanceResponse(balance = savedPoint.balance)
    }

    private fun findPoint(userId: Long): Point {
        return pointRepository.findByUserId(userId) ?: throw PointNotFound()
    }
}
