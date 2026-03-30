package devcoop.occount.item.application.usecase.delete

import devcoop.occount.item.application.output.ItemRepository
import devcoop.occount.item.domain.item.ItemNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeleteItemUseCase(
    private val itemRepository: ItemRepository,
) {
    @Transactional
    fun delete(id: Long) {
        val item = itemRepository.findById(id)
            ?: throw ItemNotFoundException()

        itemRepository.save(item.deactivate())
    }
}
