package devcoop.occount.payment.infrastructure.persistence.paymentlog

import devcoop.occount.payment.domain.payment.EventType
import devcoop.occount.payment.domain.payment.PaymentLog
import devcoop.occount.payment.domain.payment.PaymentType
import devcoop.occount.payment.domain.payment.RefundState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PaymentLogPersistenceMapperTest {
    @Test
    fun `card payment entity gets zero point transaction columns`() {
        val entity = PaymentLogPersistenceMapper.toEntity(
            PaymentLog(
                userId = null,
                paymentType = PaymentType.CARD,
                totalAmount = 1000,
                eventType = EventType.NONE,
                refundState = RefundState.NONE,
            ),
        )

        val pointTransaction = entity.getPointTransaction()

        requireNotNull(pointTransaction)
        assertEquals(0, pointTransaction.getBeforePoint())
        assertEquals(0, pointTransaction.getChangePoint())
        assertEquals(0, pointTransaction.getAfterPoint())
    }

    @Test
    fun `point payment without point transaction keeps null embeddable`() {
        val entity = PaymentLogPersistenceMapper.toEntity(
            PaymentLog(
                userId = 1L,
                paymentType = PaymentType.POINT,
                totalAmount = 1000,
                eventType = EventType.NONE,
                refundState = RefundState.NONE,
            ),
        )

        assertNull(entity.getPointTransaction())
    }

    @Test
    fun `card payment entity is restored without point transaction in domain`() {
        val domain = PaymentLogPersistenceMapper.toDomain(
            PaymentLogJpaEntity(
                userId = null,
                paymentType = PaymentType.CARD,
                totalAmount = 1000,
                pointTransaction = devcoop.occount.payment.infrastructure.persistence.chargelog.PointTransactionJpaEmbeddable(
                    beforePoint = 0,
                    changeAmount = 0,
                    afterPoint = 0,
                ),
                eventType = EventType.NONE,
                refundState = RefundState.NONE,
            ),
        )

        assertNull(domain.getPointTransaction())
    }
}
