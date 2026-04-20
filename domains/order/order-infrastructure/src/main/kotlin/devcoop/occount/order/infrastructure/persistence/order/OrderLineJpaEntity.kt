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
@Table(name = "order_lines")
class OrderLineJpaEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private var order: OrderJpaEntity? = null,
    @Column(name = "item_id", nullable = false)
    private var itemId: Long = 0L,
    @Column(name = "item_name_snapshot", nullable = false)
    private var itemNameSnapshot: String = "",
    @Column(name = "unit_price", nullable = false)
    private var unitPrice: Int = 0,
    @Column(name = "quantity", nullable = false)
    private var quantity: Int = 0,
    @Column(name = "total_price", nullable = false)
    private var totalPrice: Int = 0,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private var id: Long = 0L,
) {
    fun getItemId() = itemId
    fun getItemNameSnapshot() = itemNameSnapshot
    fun getUnitPrice() = unitPrice
    fun getQuantity() = quantity
    fun getTotalPrice() = totalPrice
}
