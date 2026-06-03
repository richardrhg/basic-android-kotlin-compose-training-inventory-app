package com.example.inventory

import androidx.lifecycle.SavedStateHandle
import com.example.inventory.data.Item
import com.example.inventory.data.ItemsRepository
import com.example.inventory.ui.item.ItemDetailsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * ItemDetailsViewModel 的 Unit Test。
 * 使用 FakeItemsRepository 取代真實的 Room Repository。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ItemDetailsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 測試 1：reduceQuantityByOne — quantity > 0
    @Test
    fun reduceQuantityByOne_whenQuantityGreaterThanZero_updatesItemWithDecrementedQuantity() =
        runTest {
            val fakeRepository = FakeItemsRepository()
            val testItem = Item(id = 1, name = "Apples", price = 10.0, quantity = 5)
            fakeRepository.insertItem(testItem)

            val savedStateHandle = SavedStateHandle(mapOf("itemId" to 1))
            val viewModel = ItemDetailsViewModel(savedStateHandle, fakeRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            advanceUntilIdle()

            viewModel.reduceQuantityByOne()
            advanceUntilIdle()

            assertEquals(1, fakeRepository.updatedItems.size)
            assertEquals(4, fakeRepository.updatedItems[0].quantity)
        }

    // 測試 2：reduceQuantityByOne — quantity == 0
    @Test
    fun reduceQuantityByOne_whenQuantityIsZero_doesNotCallUpdateItem() = runTest {
        val fakeRepository = FakeItemsRepository()
        val testItem = Item(id = 1, name = "Apples", price = 10.0, quantity = 0)
        fakeRepository.insertItem(testItem)

        val savedStateHandle = SavedStateHandle(mapOf("itemId" to 1))
        val viewModel = ItemDetailsViewModel(savedStateHandle, fakeRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.reduceQuantityByOne()
        advanceUntilIdle()

        assertEquals(0, fakeRepository.updatedItems.size)
    }

    // 測試 3：deleteItem
    @Test
    fun deleteItem_callsRepositoryDeleteItem() = runTest {
        val fakeRepository = FakeItemsRepository()
        val testItem = Item(id = 1, name = "Apples", price = 10.0, quantity = 5)
        fakeRepository.insertItem(testItem)

        val savedStateHandle = SavedStateHandle(mapOf("itemId" to 1))
        val viewModel = ItemDetailsViewModel(savedStateHandle, fakeRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.deleteItem()

        assertEquals(1, fakeRepository.deletedItems.size)
        assertEquals(testItem, fakeRepository.deletedItems[0])
    }
}

class FakeItemsRepository : ItemsRepository {

    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val updatedItems = mutableListOf<Item>()
    val deletedItems = mutableListOf<Item>()

    override fun getAllItemsStream(): Flow<List<Item>> = _items

    override fun getItemStream(id: Int): Flow<Item?> =
        _items.map { list -> list.find { it.id == id } }

    override suspend fun insertItem(item: Item) {
        _items.value = _items.value + item
    }

    override suspend fun updateItem(item: Item) {
        updatedItems.add(item)
        _items.value = _items.value.map { if (it.id == item.id) item else it }
    }

    override suspend fun deleteItem(item: Item) {
        deletedItems.add(item)
        _items.value = _items.value.filter { it.id != item.id }
    }
}
