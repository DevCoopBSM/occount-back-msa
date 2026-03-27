package devcoop.occount.payment.application.dto.request

import com.fasterxml.jackson.annotation.JsonProperty

data class PgCommand(
    val amount: Int,
    @param:JsonProperty("products")
    val items: List<ItemCommand>
)
