package devcoop.occount.point.infrastructure.event

import devcoop.occount.core.common.event.DomainEventHeaders
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.db.outbox.ConsumedEventJpaEntity
import devcoop.occount.db.outbox.ConsumedEventRepository
import devcoop.occount.point.application.usecase.initialize.InitializePointUseCase
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.Instant

@Component
class MemberRegisteredPointInitializer(
    private val objectMapper: ObjectMapper,
    private val initializePointUseCase: InitializePointUseCase,
    private val consumedEventRepository: ConsumedEventRepository,
) {
    @Transactional
    @KafkaListener(
        topics = [DomainTopics.MEMBER_REGISTERED],
        groupId = "point-initializer-v1",
    )
    fun onMemberRegistered(
        payload: String,
        @Header(DomainEventHeaders.EVENT_ID) eventId: String,
    ) {
        val consumerName = "point-initializer-v1"
        if (isProcessed(consumerName, eventId)) {
            return
        }

        val event = objectMapper.readValue<MemberRegisteredEvent>(payload)
        initializePointUseCase.initialize(event.userId)

        markProcessed(consumerName, eventId)
    }

    private fun isProcessed(consumerName: String, eventId: String): Boolean {
        return consumedEventRepository.existsById(processedEventId(consumerName, eventId))
    }

    private fun markProcessed(consumerName: String, eventId: String) {
        consumedEventRepository.save(
            ConsumedEventJpaEntity(
                id = processedEventId(consumerName, eventId),
                consumerName = consumerName,
                eventId = eventId,
                processedAt = Instant.now(),
            ),
        )
    }

    private fun processedEventId(consumerName: String, eventId: String): String {
        return "$consumerName:$eventId"
    }
}
