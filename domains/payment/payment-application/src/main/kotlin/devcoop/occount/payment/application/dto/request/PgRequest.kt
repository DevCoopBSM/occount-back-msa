package devcoop.occount.payment.application.dto.request

import com.fasterxml.jackson.annotation.JsonProperty

data class PgRequest(
    val amount: Int,
    @param:JsonProperty("products")
    val items: List<ItemInfo>
)
