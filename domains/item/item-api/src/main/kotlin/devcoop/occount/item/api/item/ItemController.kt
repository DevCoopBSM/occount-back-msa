package devcoop.occount.item.api.item

import devcoop.occount.item.application.query.ItemCategoryListResponse
import devcoop.occount.item.application.query.ItemListResponse
import devcoop.occount.item.application.query.ItemLookupListResponse
import devcoop.occount.item.application.query.ItemQueryService
import devcoop.occount.item.application.shared.ItemLookupResponse
import devcoop.occount.item.application.shared.ItemResponse
import devcoop.occount.item.application.usecase.delete.DeleteItemUseCase
import devcoop.occount.item.application.usecase.sync.SyncItemsFromTossUseCase
import devcoop.occount.item.application.usecase.update.ItemUpdateRequest
import devcoop.occount.item.application.usecase.update.UpdateItemUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/items")
@RestController
class ItemController(
    private val itemQueryService: ItemQueryService,
    private val syncItemsFromTossUseCase: SyncItemsFromTossUseCase,
    private val updateItemUseCase: UpdateItemUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
) {
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    fun getAllItems(): ItemListResponse {
        return itemQueryService.getAllItems()
    }

    @GetMapping("/categories")
    @ResponseStatus(HttpStatus.OK)
    fun getItemCategories(): ItemCategoryListResponse {
        return itemQueryService.getItemCategories()
    }

    @GetMapping("/without-barcode")
    @ResponseStatus(HttpStatus.OK)
    fun getItemsWithoutBarcode(): ItemLookupListResponse {
        return itemQueryService.getItemsWithoutBarcode()
    }

    @GetMapping("/by-ids")
    @ResponseStatus(HttpStatus.OK)
    fun getItemsByIds(@RequestParam ids: List<Long>): ItemLookupListResponse {
        return itemQueryService.getItemsByIds(ids)
    }

    @GetMapping("/{barcode}")
    @ResponseStatus(HttpStatus.OK)
    fun getItemByBarcode(@PathVariable barcode: String): ItemLookupResponse {
        return itemQueryService.getItemByBarcode(barcode)
    }

    @PostMapping("/sync")
    @ResponseStatus(HttpStatus.OK)
    fun syncItemsFromToss() {
        syncItemsFromTossUseCase.sync()
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    fun updateItem(
        @PathVariable id: Long,
        @RequestBody request: ItemUpdateRequest,
    ): ItemResponse {
        return updateItemUseCase.update(id, request)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteItem(@PathVariable id: Long) {
        deleteItemUseCase.delete(id)
    }
}
