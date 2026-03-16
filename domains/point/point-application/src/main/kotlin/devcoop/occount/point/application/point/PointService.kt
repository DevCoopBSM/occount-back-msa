package devcoop.occount.point.application.point

import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.point.domain.Point
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PointService(
    private val pointRepository: PointRepository,
    private val eventPublisher: EventPublisher,
) {
    private companion object {
        const val POINT_INITIALIZED_EVENT_TYPE = "PointInitializedEvent"
        const val POINT_BALANCE_CHANGED_EVENT_TYPE = "PointBalanceChangedEvent"
    }

    @Transactional
    fun initialize(userId: Long): Point {
        pointRepository.findByUserId(userId)?.let { return it }

        return pointRepository.save(Point(userId = userId)).also { savedPoint ->
            publishInitialized(savedPoint)
        }
    }

    fun getBalance(userId: Long): PointBalanceResponse {
        val balance = pointRepository.findByUserId(userId)?.balance ?: 0
        return PointBalanceResponse(userId = userId, balance = balance)
    }

    @Transactional
    fun charge(userId: Long, amount: Int): PointBalanceResponse {
        val savedPoint = pointRepository.save(loadOrCreate(userId).charge(amount))
        publishBalanceChanged(savedPoint, amount)
        return PointBalanceResponse(userId = savedPoint.userId, balance = savedPoint.balance)
    }

    @Transactional
    fun deduct(userId: Long, amount: Int): PointBalanceResponse {
        val savedPoint = pointRepository.save(loadOrCreate(userId).deduct(amount))
        publishBalanceChanged(savedPoint, -amount)
        return PointBalanceResponse(userId = savedPoint.userId, balance = savedPoint.balance)
    }

    private fun loadOrCreate(userId: Long): Point {
        return pointRepository.findByUserId(userId) ?: Point(userId = userId)
    }

    private fun publishInitialized(point: Point) {
        eventPublisher.publish(
            topic = DomainTopics.POINT_INITIALIZED,
            key = point.userId.toString(),
            eventType = POINT_INITIALIZED_EVENT_TYPE,
            payload = mapOf(
                "userId" to point.userId,
                "balance" to point.balance,
            ),
        )
    }

    private fun publishBalanceChanged(point: Point, changedAmount: Int) {
        eventPublisher.publish(
            topic = DomainTopics.POINT_BALANCE_CHANGED,
            key = point.userId.toString(),
            eventType = POINT_BALANCE_CHANGED_EVENT_TYPE,
            payload = mapOf(
                "userId" to point.userId,
                "balance" to point.balance,
                "changedAmount" to changedAmount,
            ),
        )
    }
}
