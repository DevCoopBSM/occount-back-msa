package devcoop.occount.item.api.item

import devcoop.occount.item.api.support.FakeItemRepository
import devcoop.occount.item.api.support.itemFixture
import devcoop.occount.item.api.support.mockMvc
import devcoop.occount.item.api.support.TestTransactionManager
import devcoop.occount.item.application.query.ItemQueryService
import devcoop.occount.item.application.usecase.create.CreateItemUseCase
import devcoop.occount.item.application.usecase.delete.DeleteItemUseCase
import devcoop.occount.item.application.usecase.update.UpdateItemUseCase
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class ItemControllerTest {
    @Test
    fun `get all items returns active item list`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(
                itemFixture(itemId = 1L, name = "Snack", quantity = 3),
                itemFixture(itemId = 2L, name = "Hidden", isActive = false),
            ),
        )
        val mockMvc = mockMvc(controller(itemRepository))

        mockMvc.perform(get("/items"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].itemId").value(1))
            .andExpect(jsonPath("$.items[0].name").value("Snack"))
            .andExpect(jsonPath("$.items[0].quantity").value(3))
    }

    @Test
    fun `get item categories returns category list`() {
        val mockMvc = mockMvc(controller(FakeItemRepository()))

        mockMvc.perform(get("/items/categories"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.itemCategories.length()").isNotEmpty)
    }

    @Test
    fun `get items without barcode returns only items without barcode`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(
                itemFixture(itemId = 1L, name = "Snack", barcode = null),
                itemFixture(itemId = 2L, name = "Drink", barcode = "88012341234"),
            ),
        )
        val mockMvc = mockMvc(controller(itemRepository))

        mockMvc.perform(get("/items/without-barcode"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].itemId").value(1))
    }

    @Test
    fun `get items by ids returns requested items`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(
                itemFixture(itemId = 1L, name = "Snack"),
                itemFixture(itemId = 2L, name = "Drink"),
            ),
        )
        val mockMvc = mockMvc(controller(itemRepository))

        mockMvc.perform(get("/items/by-ids").param("ids", "2", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(2))
    }

    @Test
    fun `search items returns matching active items`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(
                itemFixture(itemId = 1L, name = "초코 과자"),
                itemFixture(itemId = 2L, name = "딸기 우유"),
                itemFixture(itemId = 3L, name = "초코 우유"),
                itemFixture(itemId = 4L, name = "초코 비활성", isActive = false),
            ),
        )
        val mockMvc = mockMvc(controller(itemRepository))

        mockMvc.perform(get("/items/search").param("q", "초코"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(2))
    }

    @Test
    fun `get item by barcode returns lookup response`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(
                itemFixture(itemId = 1L, name = "Snack", barcode = "88012341234"),
            ),
        )
        val mockMvc = mockMvc(controller(itemRepository))

        mockMvc.perform(get("/items/88012341234"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.itemId").value(1))
            .andExpect(jsonPath("$.barcode").value("88012341234"))
    }

    @Test
    fun `get item by barcode returns 404 when item does not exist`() {
        val mockMvc = mockMvc(controller(FakeItemRepository()))

        mockMvc.perform(get("/items/88012341234"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("현재 등록되지 않은 상품입니다."))
    }

    @Test
    fun `create item returns 201 with created item response`() {
        val itemRepository = FakeItemRepository()
        val mockMvc = mockMvc(controller(itemRepository))

        mockMvc.perform(
            post("/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Snack",
                      "category": "과자",
                      "price": 1500,
                      "barcode": "88012341234",
                      "quantity": 10
                    }
                    """.trimIndent(),
                ),
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.itemId").value(1))
            .andExpect(jsonPath("$.name").value("Snack"))
            .andExpect(jsonPath("$.barcode").value("88012341234"))
    }

    @Test
    fun `update item returns updated response`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(
                itemFixture(itemId = 1L, name = "Old", quantity = 3),
            ),
        )
        val mockMvc = mockMvc(controller(itemRepository))

        mockMvc.perform(
            put("/items/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "New",
                      "category": "음료",
                      "price": 2000,
                      "barcode": "88099999999",
                      "quantity": 5
                    }
                    """.trimIndent(),
                ),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.itemId").value(1))
            .andExpect(jsonPath("$.name").value("New"))
            .andExpect(jsonPath("$.price").value(2000))
    }

    @Test
    fun `update item returns 404 when item does not exist`() {
        val mockMvc = mockMvc(controller(FakeItemRepository()))

        mockMvc.perform(
            put("/items/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "New",
                      "category": "음료",
                      "price": 2000,
                      "barcode": null,
                      "quantity": 5
                    }
                    """.trimIndent(),
                ),
        ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("현재 등록되지 않은 상품입니다."))
    }

    @Test
    fun `delete item returns 204`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(
                itemFixture(itemId = 1L, name = "Snack"),
            ),
        )
        val mockMvc = mockMvc(controller(itemRepository))

        mockMvc.perform(delete("/items/1"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `delete item returns 404 when item does not exist`() {
        val mockMvc = mockMvc(controller(FakeItemRepository()))

        mockMvc.perform(delete("/items/1"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("현재 등록되지 않은 상품입니다."))
    }

    private fun controller(itemRepository: FakeItemRepository): ItemController {
        return ItemController(
            itemQueryService = ItemQueryService(itemRepository),
            createItemUseCase = CreateItemUseCase(itemRepository, TestTransactionManager()),
            updateItemUseCase = UpdateItemUseCase(itemRepository, TestTransactionManager()),
            deleteItemUseCase = DeleteItemUseCase(itemRepository),
        )
    }
}
