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

    /**
     * 【HW3 要求 2 - 測試 1】reduceQuantityByOne() 在 quantity > 0 時，會更新 repository。
     *
     * 預期：呼叫一次 reduceQuantityByOne() 後，updateItem() 被呼叫一次，
     *       且傳入的 item 數量由 5 減為 4。
     *
     * 註：uiState 是 stateIn(WhileSubscribed) 的 StateFlow，「沒有訂閱者時不會計算」。
     *    因此先用 backgroundScope 訂閱 uiState，確保 ViewModel 內部狀態（含目前 item）
     *    被正確初始化，reduceQuantityByOne() 才拿得到正確的當前數量。
     */
    @Test
    fun reduceQuantityByOne_whenQuantityGreaterThanZero_updatesItemWithDecrementedQuantity() =
        runTest {
            // Arrange：放入一筆 quantity=5 的資料，並建立指向它的 ViewModel
            val fakeRepository = FakeItemsRepository()
            val testItem = Item(id = 1, name = "Apples", price = 10.0, quantity = 5)
            fakeRepository.insertItem(testItem)

            val savedStateHandle = SavedStateHandle(mapOf("itemId" to 1))
            val viewModel = ItemDetailsViewModel(savedStateHandle, fakeRepository)

            // 訂閱 uiState 以觸發 WhileSubscribed 狀態計算
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            advanceUntilIdle()

            // Act：減少數量
            viewModel.reduceQuantityByOne()
            advanceUntilIdle()

            // Assert：updateItem 被呼叫一次，且數量由 5 → 4
            assertEquals(1, fakeRepository.updatedItems.size)
            assertEquals(4, fakeRepository.updatedItems[0].quantity)
        }

    /**
     * 【HW3 要求 2 - 測試 2】reduceQuantityByOne() 在 quantity == 0 時，不呼叫 updateItem()。
     *
     * 預期：當數量已為 0，再呼叫 reduceQuantityByOne() 應被 ViewModel 內部的
     *       「if (quantity > 0)」守門條件擋下，完全不更新 repository。
     */
    @Test
    fun reduceQuantityByOne_whenQuantityIsZero_doesNotCallUpdateItem() = runTest {
        // Arrange：放入一筆 quantity=0 的資料
        val fakeRepository = FakeItemsRepository()
        val testItem = Item(id = 1, name = "Apples", price = 10.0, quantity = 0)
        fakeRepository.insertItem(testItem)

        val savedStateHandle = SavedStateHandle(mapOf("itemId" to 1))
        val viewModel = ItemDetailsViewModel(savedStateHandle, fakeRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        // Act：對數量為 0 的項目嘗試減少
        viewModel.reduceQuantityByOne()
        advanceUntilIdle()

        // Assert：updateItem 完全沒有被呼叫
        assertEquals(0, fakeRepository.updatedItems.size)
    }

    /**
     * 【HW3 要求 2 - 測試 3】deleteItem() 正確呼叫 itemsRepository.deleteItem()。
     *
     * 預期：呼叫 viewModel.deleteItem() 後，repository 的 deleteItem() 被呼叫一次，
     *       且刪除的對象正是目前畫面上的那筆 item。
     */
    @Test
    fun deleteItem_callsRepositoryDeleteItem() = runTest {
        // Arrange
        val fakeRepository = FakeItemsRepository()
        val testItem = Item(id = 1, name = "Apples", price = 10.0, quantity = 5)
        fakeRepository.insertItem(testItem)

        val savedStateHandle = SavedStateHandle(mapOf("itemId" to 1))
        val viewModel = ItemDetailsViewModel(savedStateHandle, fakeRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        // Act：刪除目前項目
        viewModel.deleteItem()

        // Assert：deleteItem 被呼叫一次，且刪除的是正確的 item
        assertEquals(1, fakeRepository.deletedItems.size)
        assertEquals(testItem, fakeRepository.deletedItems[0])
    }
}

/**
 * 假的 ItemsRepository 實作，用於要求 2 的 ViewModel 單元測試。
 *
 * 以記憶體中的 MutableStateFlow 模擬 Room 的 Flow 行為，
 * 讓測試完全不依賴真實資料庫（純 JVM、執行極快）。
 * 另用 updatedItems / deletedItems 兩個清單記錄被呼叫的情形，
 * 方便測試驗證「updateItem / deleteItem 是否被呼叫、以及傳入什麼」。
 */
class FakeItemsRepository : ItemsRepository {

    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val updatedItems = mutableListOf<Item>()   // 記錄每次 updateItem 傳入的 item
    val deletedItems = mutableListOf<Item>()   // 記錄每次 deleteItem 傳入的 item

    override fun getAllItemsStream(): Flow<List<Item>> = _items

    override fun getItemStream(id: Int): Flow<Item?> =
        _items.map { list -> list.find { it.id == id } }

    override suspend fun insertItem(item: Item) {
        _items.value = _items.value + item
    }

    override suspend fun updateItem(item: Item) {
        updatedItems.add(item)   // 記錄被呼叫
        _items.value = _items.value.map { if (it.id == item.id) item else it }
    }

    override suspend fun deleteItem(item: Item) {
        deletedItems.add(item)   // 記錄被呼叫
        _items.value = _items.value.filter { it.id != item.id }
    }
}
