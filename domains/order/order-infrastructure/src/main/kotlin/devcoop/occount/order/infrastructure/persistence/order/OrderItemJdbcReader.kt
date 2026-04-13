package devcoop.occount.order.infrastructure.persistence.order

import devcoop.occount.order.application.output.OrderItemData
import devcoop.occount.order.application.output.OrderItemReader
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class OrderItemJdbcReader(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : OrderItemReader {
    override fun findByIds(itemIds: Set<Long>): List<OrderItemData> {
        if (itemIds.isEmpty()) {
            return emptyList()
        }

        return jdbcTemplate.query(
            """
            select item_id, name, price, is_active
            from item
            where item_id in (:itemIds)
            """.trimIndent(),
            MapSqlParameterSource("itemIds", itemIds),
        ) { rs, _ ->
            OrderItemData(
                itemId = rs.getLong("item_id"),
                itemName = rs.getString("name"),
                itemPrice = rs.getInt("price"),
                isActive = rs.getBoolean("is_active"),
            )
        }
    }
}
