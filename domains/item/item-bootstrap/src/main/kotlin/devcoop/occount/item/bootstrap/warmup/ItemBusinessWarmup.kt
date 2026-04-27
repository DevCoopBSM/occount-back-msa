package devcoop.occount.item.bootstrap.warmup

import devcoop.occount.item.application.output.ItemRepository
import devcoop.occount.item.application.query.ItemQueryService
import devcoop.occount.warmup.BusinessWarmup
import devcoop.occount.warmup.WarmupProbe
import org.springframework.stereotype.Component

@Component
class ItemBusinessWarmup(
    private val itemQueryService: ItemQueryService,
    private val itemRepository: ItemRepository,
) : BusinessWarmup {

    override fun warmup() {
        itemQueryService.getAllItems()
        itemQueryService.getItemsWithoutBarcode()
        itemRepository.findByBarcode(WarmupProbe.BARCODE)
        itemRepository.findById(WarmupProbe.USER_ID)
    }
}
