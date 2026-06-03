/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.inventory

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.inventory.data.InventoryDatabase
import com.example.inventory.data.Item
import com.example.inventory.data.ItemDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class ItemDaoTest {

    private lateinit var itemDao: ItemDao
    private lateinit var inventoryDatabase: InventoryDatabase
    private val item1 = Item(1, "Apples", 10.0, 20)
    private val item2 = Item(2, "Bananas", 15.0, 97)

    @Before
    fun createDb() {
        val context: Context = ApplicationProvider.getApplicationContext()
        inventoryDatabase = Room.inMemoryDatabaseBuilder(context, InventoryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        itemDao = inventoryDatabase.itemDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        inventoryDatabase.close()
    }

    @Test
    @Throws(Exception::class)
    fun daoInsert_insertsItemIntoDB() = runBlocking {
        addOneItemToDb()
        val allItems = itemDao.getAllItems().first()
        assertEquals(allItems[0], item1)
    }

    @Test
    @Throws(Exception::class)
    fun daoGetAllItems_returnsAllItemsFromDB() = runBlocking {
        addTwoItemsToDb()
        val allItems = itemDao.getAllItems().first()
        assertEquals(allItems[0], item1)
        assertEquals(allItems[1], item2)
    }

    @Test
    @Throws(Exception::class)
    fun daoGetItem_returnsItemFromDB() = runBlocking {
        addOneItemToDb()
        val item = itemDao.getItem(1)
        assertEquals(item.first(), item1)
    }

    @Test
    @Throws(Exception::class)
    fun daoDeleteItems_deletesAllItemsFromDB() = runBlocking {
        addTwoItemsToDb()
        itemDao.delete(item1)
        itemDao.delete(item2)
        val allItems = itemDao.getAllItems().first()
        assertTrue(allItems.isEmpty())
    }

    @Test
    @Throws(Exception::class)
    fun daoUpdateItems_updatesItemsInDB() = runBlocking {
        addTwoItemsToDb()
        itemDao.update(Item(1, "Apples", 15.0, 25))
        itemDao.update(Item(2, "Bananas", 5.0, 50))
        val allItems = itemDao.getAllItems().first()
        assertEquals(allItems[0], Item(1, "Apples", 15.0, 25))
        assertEquals(allItems[1], Item(2, "Bananas", 5.0, 50))
    }

    /**
     * 【HW3 要求 1】插入重複 ID，驗證只保留原先第一筆資料。
     *
     * 情境：Room 的 @Insert 預設衝突策略（OnConflictStrategy.IGNORE）在遇到
     *       主鍵重複時，會「靜默忽略」第二筆、不丟出任何例外，資料庫只保留先插入的那筆。
     *
     * 步驟：
     *   1. 先插入 item1（id=1, Apples, 10.0, 20）。
     *   2. 再插入一筆「相同 id=1、但內容完全不同」的 duplicateItem。
     *   3. 取出資料庫所有資料來檢查。
     *
     * 預期：資料庫只有 1 筆，且內容仍為最初的 item1（第二筆被忽略，不覆蓋、不報錯）。
     */
    @Test
    @Throws(Exception::class)
    fun insertDuplicateId_keepsOriginalItem() = runBlocking {
        // 1. 插入第一筆原始資料
        itemDao.insert(item1)
        // 2. 插入相同 ID 但內容不同的第二筆（應被 Room 靜默忽略）
        val duplicateItem = Item(id = 1, name = "Different Apple", price = 99.0, quantity = 999)
        itemDao.insert(duplicateItem)
        // 3. 取出全部資料進行驗證
        val allItems = itemDao.getAllItems().first()
        assertEquals(1, allItems.size)              // 只應有 1 筆（第二筆未被加入）
        assertEquals(item1, allItems[0])            // 保留的是最初的 item1
        assertEquals("Apples", allItems[0].name)    // 名稱仍為原始值，未被覆蓋
        assertEquals(10.0, allItems[0].price, 0.001)
        assertEquals(20, allItems[0].quantity)
    }

    /**
     * 【HW3 要求 3】自動新增「學號」資料並確認新增成功。
     *
     * 自動插入一筆 name=學號(B11330218)、price=100、quantity=10 的資料，
     * 再從資料庫查回，逐欄驗證皆正確儲存，以此確認新增成功。
     *
     * 備註：id 刻意設為 0，讓 Room 的 @PrimaryKey(autoGenerate = true) 自動產生主鍵，
     *       避免與其他測試固定的 id 衝突；驗證時改以唯一的 name 查找，
     *       避開自動產生 id 不確定的問題。
     */
    @Test
    @Throws(Exception::class)
    fun insertStudentIdItem_verifyStoredInDatabase() = runBlocking {
        // 1. 自動新增一筆以學號為名稱的資料
        val studentItem = Item(id = 0, name = "B11330218", price = 100.0, quantity = 10)
        itemDao.insert(studentItem)
        // 2. 從資料庫以名稱(學號)查回該筆資料
        val allItems = itemDao.getAllItems().first()
        val insertedItem = allItems.find { it.name == "B11330218" }
        // 3. 確認新增成功：資料存在且三個欄位皆正確
        assertNotNull("Item with name B11330218 should exist in DB", insertedItem)
        assertEquals("B11330218", insertedItem!!.name)
        assertEquals(100.0, insertedItem.price, 0.001)
        assertEquals(10, insertedItem.quantity)
    }

    private suspend fun addOneItemToDb() {
        itemDao.insert(item1)
    }

    private suspend fun addTwoItemsToDb() {
        itemDao.insert(item1)
        itemDao.insert(item2)
    }
}
