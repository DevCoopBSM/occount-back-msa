package devcoop.occount.order.infrastructure.persistence.order

import devcoop.occount.order.domain.order.OrderAggregate
import devcoop.occount.order.domain.order.OrderLine
import devcoop.occount.order.domain.order.OrderPayment
import devcoop.occount.order.domain.order.OrderPaymentResult

object OrderPersistenceMapper {
    fun toDomain(entity: OrderJpaEntity): OrderAggregate {
        return OrderAggregate(
            orderId = entity.getOrderId(),
            userId = entity.getUserId(),
            lines = entity.getLines().map(::toDomainLine),
            payment = OrderPayment(
                type = entity.getPaymentType(),
                totalAmount = entity.getTotalAmount(),
            ),
            status = entity.getStatus(),
            paymentStatus = entity.getPaymentStatus(),
            stockStatus = entity.getStockStatus(),
            cancelRequested = entity.isCancelRequested(),
            failureReason = entity.getFailureReason(),
            expiresAt = entity.getExpiresAt(),
            paymentResult = OrderPaymentResult(
                paymentLogId = entity.getPaymentLogId(),
                pointsUsed = entity.getPointsUsed(),
                cardAmount = entity.getCardAmount(),
                transactionId = entity.getTransactionId(),
                approvalNumber = entity.getApprovalNumber(),
            ),
            paymentCompensationRequested = entity.isPaymentCompensationRequested(),
            stockCompensationRequested = entity.isStockCompensationRequested(),
            version = entity.getVersion(),
        )
    }

    fun toEntity(domain: OrderAggregate): OrderJpaEntity {
        val entity = OrderJpaEntity(
            orderId = domain.orderId,
            userId = domain.userId,
            paymentType = domain.payment.type,
            totalAmount = domain.payment.totalAmount,
            status = domain.status,
            paymentStatus = domain.paymentStatus,
            stockStatus = domain.stockStatus,
            cancelRequested = domain.cancelRequested,
            failureReason = domain.failureReason,
            expiresAt = domain.expiresAt,
            paymentLogId = domain.paymentResult.paymentLogId,
            pointsUsed = domain.paymentResult.pointsUsed,
            cardAmount = domain.paymentResult.cardAmount,
            transactionId = domain.paymentResult.transactionId,
            approvalNumber = domain.paymentResult.approvalNumber,
            paymentCompensationRequested = domain.paymentCompensationRequested,
            stockCompensationRequested = domain.stockCompensationRequested,
            version = domain.version,
        )

        val lines = domain.lines.map { toEntityLine(entity, it) }.toMutableList()
        entity.replaceLines(lines)

        return entity
    }

    private fun toDomainLine(entity: OrderLineJpaEntity): OrderLine {
        return OrderLine(
            itemId = entity.getItemId(),
            itemNameSnapshot = entity.getItemNameSnapshot(),
            unitPrice = entity.getUnitPrice(),
            quantity = entity.getQuantity(),
            totalPrice = entity.getTotalPrice(),
        )
    }

    private fun toEntityLine(order: OrderJpaEntity, domain: OrderLine): OrderLineJpaEntity {
        return OrderLineJpaEntity(
            order = order,
            itemId = domain.itemId,
            itemNameSnapshot = domain.itemNameSnapshot,
            unitPrice = domain.unitPrice,
            quantity = domain.quantity,
            totalPrice = domain.totalPrice,
        )
    }
}
