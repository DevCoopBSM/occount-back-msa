package devcoop.occount.db.outbox

import org.springframework.dao.DataIntegrityViolationException

/**
 * 컨슈머 멱등성을 보조하는 트래커.
 *
 * - [isProcessed]로 사전 체크하거나
 * - [recordOrThrowOnDuplicate]로 처리 마커를 저장하면서 PK 충돌 시 [onDuplicate]를 던질 수 있다.
 *
 * 컨슈머 단위로 인스턴스화하여 [consumerName]을 prefix 삼아 다른 컨슈머와 멱등 키가 충돌하지 않게 한다.
 */
class IdempotencyTracker(
    private val consumerName: String,
    private val consumedEventRepository: ConsumedEventRepository,
) {
    fun isProcessed(eventId: String): Boolean {
        return consumedEventRepository.existsById(consumedId(eventId))
    }

    fun recordOrThrowOnDuplicate(eventId: String, onDuplicate: () -> Nothing) {
        try {
            consumedEventRepository.save(
                ConsumedEventJpaEntity(
                    id = consumedId(eventId),
                    consumerName = consumerName,
                    eventId = eventId,
                ),
            )
        } catch (_: DataIntegrityViolationException) {
            onDuplicate()
        }
    }

    private fun consumedId(eventId: String): String = "$consumerName:$eventId"
}
