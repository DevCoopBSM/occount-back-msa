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
@Table(name = "requested_order_lines")
class RequestedOrderLineJpaEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private var order: OrderJpaEntity? = null,
    @Column(name = "item_id", nullable = false)
    private var itemId: Long = 0L,
    @Column(name = "quantity", nullable = false)
    private var quantity: Int = 0,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private var id: Long = 0L,
) {
    fun getItemId() = itemId
    fun getQuantity() = quantity
}
