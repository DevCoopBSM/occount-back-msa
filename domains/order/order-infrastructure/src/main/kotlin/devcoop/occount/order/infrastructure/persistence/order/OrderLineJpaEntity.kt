package devcoop.occount.order.infrastructure.persistence.order

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "order_line")
class OrderLineJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @field:Column(name = "order_line_id")
    private var orderLineId: Long = 0L,
    @field:ManyToOne(fetch = FetchType.LAZY, optional = false)
    @field:JoinColumn(name = "order_id", nullable = false)
    private var order: OrderJpaEntity,
    @field:Column(name = "item_id", nullable = false)
    private var itemId: Long = 0L,
    @field:Column(name = "item_name_snapshot", nullable = false)
    private var itemNameSnapshot: String = "",
    @field:Column(name = "unit_price", nullable = false)
    private var unitPrice: Int = 0,
    @field:Column(name = "quantity", nullable = false)
    private var quantity: Int = 0,
    @field:Column(name = "total_price", nullable = false)
    private var totalPrice: Int = 0,
) {
    fun getItemId() = itemId
    fun getItemNameSnapshot() = itemNameSnapshot
    fun getUnitPrice() = unitPrice
    fun getQuantity() = quantity
    fun getTotalPrice() = totalPrice
}
