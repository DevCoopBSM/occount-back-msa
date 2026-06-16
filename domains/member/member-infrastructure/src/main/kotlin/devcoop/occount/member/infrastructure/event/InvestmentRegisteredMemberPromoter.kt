package devcoop.occount.member.infrastructure.event

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import devcoop.occount.core.common.event.DomainEventHeaders
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.db.outbox.ConsumedEventJpaEntity
import devcoop.occount.db.outbox.ConsumedEventRepository
import devcoop.occount.member.application.usecase.promote.PromoteToMemberUseCase
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 출자 등록 이벤트를 받아 회원을 조합원으로 승격하는 컨슈머.
 * 멱등성은 consumed_event 로 보장하며, 승격 자체도 멱등이라 재처리에 안전하다.
 */
@Component
class InvestmentRegisteredMemberPromoter(
    private val promoteToMemberUseCase: PromoteToMemberUseCase,
    private val consumedEventRepository: ConsumedEventRepository,
) {
    private val objectMapper = jacksonObjectMapper()

    @Transactional
    @KafkaListener(
        topics = [DomainTopics.INVESTMENT_REGISTERED],
        groupId = CONSUMER_NAME,
    )
    fun onInvestmentRegistered(
        payload: String,
        @Header(DomainEventHeaders.EVENT_ID) eventId: String,
    ) {
        if (isProcessed(eventId)) {
            return
        }

        val event = objectMapper.readValue<InvestmentRegisteredEvent>(payload)
        promoteToMemberUseCase.promote(event.userId)

        markProcessed(eventId)
    }

    private fun isProcessed(eventId: String): Boolean {
        return consumedEventRepository.existsById(processedEventId(eventId))
    }

    private fun markProcessed(eventId: String) {
        consumedEventRepository.save(
            ConsumedEventJpaEntity(
                id = processedEventId(eventId),
                consumerName = CONSUMER_NAME,
                eventId = eventId,
                processedAt = Instant.now(),
            ),
        )
    }

    private fun processedEventId(eventId: String): String = "$CONSUMER_NAME:$eventId"

    companion object {
        private const val CONSUMER_NAME = "member-investment-promoter-v1"
    }
}
