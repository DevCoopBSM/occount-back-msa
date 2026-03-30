package devcoop.occount.item.application.usecase.update

import devcoop.occount.item.application.output.ItemRepository
import devcoop.occount.item.application.shared.ItemMapper
import devcoop.occount.item.application.shared.ItemResponse
import devcoop.occount.item.domain.item.ItemInfo
import devcoop.occount.item.domain.item.ItemNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateItemUseCase(
    private val itemRepository: ItemRepository,
) {
    @Transactional
    fun update(id: Long, request: ItemUpdateRequest): ItemResponse {
        val item = itemRepository.findById(id)
            ?: throw ItemNotFoundException()

        val updatedItem = item.update(
            itemInfo = ItemInfo(
                name = request.name,
                category = request.category,
                price = request.price,
                barcode = request.barcode,
            ),
            quantity = request.quantity,
        )

        return itemRepository.save(updatedItem)
            .let(ItemMapper::toResponse)
    }
}
