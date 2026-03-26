package devcoop.occount.point.application.usecase.initialize

import devcoop.occount.point.application.exception.PointAlreadyInitializedException
import devcoop.occount.point.application.output.PointRepository
import devcoop.occount.point.domain.Point
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InitializePointUseCase(
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
}
