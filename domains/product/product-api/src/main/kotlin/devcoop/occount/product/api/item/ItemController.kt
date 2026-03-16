package devcoop.occount.product.api.item

import devcoop.occount.product.application.item.ItemCategoryListResponse
import devcoop.occount.product.application.item.ItemListResponse
import devcoop.occount.product.application.item.ItemLookupListResponse
import devcoop.occount.product.application.item.ItemLookupResponse
import devcoop.occount.product.application.item.ItemResponse
import devcoop.occount.product.application.item.ItemService
import devcoop.occount.product.application.item.ItemUpdateRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/items")
@RestController
class ItemController(
    private val itemService: ItemService,
) {
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    fun getAllItems(): ItemListResponse {
        return itemService.getAllItems()
    }

    @GetMapping("/categories")
    @ResponseStatus(HttpStatus.OK)
    fun getItemCategories(): ItemCategoryListResponse {
        return itemService.getItemCategories()
    }

    @GetMapping("/without-barcode")
    @ResponseStatus(HttpStatus.OK)
    fun getItemsWithoutBarcode(): ItemLookupListResponse {
        return itemService.getItemsWithoutBarcode()
    }

    @GetMapping("/{barcode}")
    @ResponseStatus(HttpStatus.OK)
    fun getItemByBarcode(@PathVariable barcode: String): ItemLookupResponse {
        return itemService.getItemByBarcode(barcode)
    }

    @PostMapping("/sync")
    @ResponseStatus(HttpStatus.OK)
    fun syncItemsFromToss() {
        itemService.syncItemsFromToss()
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    fun updateItem(
        @PathVariable id: Long,
        @RequestBody request: ItemUpdateRequest,
    ): ItemResponse {
        return itemService.updateItem(id, request)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteItem(@PathVariable id: Long) {
        itemService.deleteItem(id)
    }
}
