package devcoop.occount.payment.infrastructure.event

import devcoop.occount.core.common.event.DomainEventHeaders
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.db.outbox.ConsumedEventJpaEntity
import devcoop.occount.db.outbox.ConsumedEventRepository
import devcoop.occount.payment.application.exception.WalletAlreadyInitializedException
import devcoop.occount.payment.application.usecase.wallet.initialize.InitializeWalletUseCase
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
    private val initializeWalletUseCase: InitializeWalletUseCase,
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
        try {
            initializeWalletUseCase.initialize(event.userId)
        } catch (_: WalletAlreadyInitializedException) {
            // 지갑이 이미 존재하면 처리된 것으로 간주
        }

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
