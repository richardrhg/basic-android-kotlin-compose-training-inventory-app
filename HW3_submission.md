# HW3 繳交文件

**姓名：** 周君諺  
**學號：** B11330218  

---

## 1. GitHub Repository

https://github.com/richardrhg/basic-android-kotlin-compose-training-inventory-app

請將以下助教帳號加為 Collaborator（Settings → Collaborators → Add people）：
- `cookiecatowo`
- `penpenpenguin`

---

## 2. 測試方法一覽

### 要求 1 — ItemDaoTest（Instrumented Test）

檔案位置：`app/src/androidTest/java/com/example/inventory/ItemDaoTest.kt`

| 測試方法名稱 | 說明 |
|---|---|
| `daoInsert_insertsItemIntoDB` | 原有：插入一筆並驗證 |
| `daoGetAllItems_returnsAllItemsFromDB` | 原有：查詢所有資料 |
| `daoGetItem_returnsItemFromDB` | 原有：依 id 查詢 |
| `daoDeleteItems_deletesAllItemsFromDB` | 原有：刪除所有資料 |
| `daoUpdateItems_updatesItemsInDB` | 原有：更新資料 |
| **`insertDuplicateId_keepsOriginalItem`** | **新增（要求1）**：插入重複 ID，驗證保留第一筆、第二筆被 IGNORE |

### 要求 2 — ItemDetailsViewModelTest（Local Unit Test）

檔案位置：`app/src/test/java/com/example/inventory/ItemDetailsViewModelTest.kt`

| 測試方法名稱 | 說明 |
|---|---|
| **`reduceQuantityByOne_whenQuantityGreaterThanZero_updatesItemWithDecrementedQuantity`** | quantity > 0 時呼叫 updateItem()，quantity 減 1 |
| **`reduceQuantityByOne_whenQuantityIsZero_doesNotCallUpdateItem`** | quantity == 0 時不可呼叫 updateItem() |
| **`deleteItem_callsRepositoryDeleteItem`** | 呼叫 deleteItem() 確認 repository.deleteItem() 被正確呼叫 |

輔助類別：`FakeItemsRepository`（同一檔案中，用 MutableStateFlow 模擬 Room Flow）

### 要求 3 — ItemDaoTest（Instrumented Test）

同一檔案：`app/src/androidTest/java/com/example/inventory/ItemDaoTest.kt`

| 測試方法名稱 | 說明 |
|---|---|
| **`insertStudentIdItem_verifyStoredInDatabase`** | 插入 name="B11330218", price=100.0, quantity=10，驗證各欄位正確儲存 |

---

## 3. 使用的 AI 工具與 Prompt

**使用工具：** Claude（Anthropic）— 透過 Cowork 模式

**Prompt 摘要：**
- 提供作業三大需求規格
- 提供參考專案連結（Google Inventory App）
- 提供學號：B11330218

Claude 分析原始專案程式碼（ItemDao.kt、ItemDetailsViewModel.kt、ItemsRepository 介面），產生完整測試程式碼。

**AI 產生後修改內容：**
- 確認 SavedStateHandle key 名稱（"itemId"）與原始碼 ItemDetailsDestination.itemIdArg 吻合
- 在 ViewModel 測試加入 backgroundScope.launch 訂閱 uiState，確保 WhileSubscribed StateFlow 正確更新
- 學號測試改用 id=0 讓 Room autoGenerate，避免 PrimaryKey 衝突

---

## 4. 如何執行測試

### 要求 1 & 3（ItemDaoTest）— 需要裝置或模擬器

```
Android Studio → 開啟 ItemDaoTest.kt → 右鍵類別名稱 → Run 'ItemDaoTest'
```

或執行特定方法：
- 要求 1：右鍵 `insertDuplicateId_keepsOriginalItem` → Run
- 要求 3：右鍵 `insertStudentIdItem_verifyStoredInDatabase` → Run

指令列：
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.inventory.ItemDaoTest
```

### 要求 2（ItemDetailsViewModelTest）— 本機 JVM，不需裝置

```
Android Studio → 開啟 ItemDetailsViewModelTest.kt → 右鍵類別名稱 → Run 'ItemDetailsViewModelTest'
```

指令列：
```bash
./gradlew test --tests "com.example.inventory.ItemDetailsViewModelTest"
```

---

## 5. 要求 3 執行截圖

> 請在 Android Studio 執行 `insertStudentIdItem_verifyStoredInDatabase` 後，
> 截取 Run 面板顯示 ✅ Tests passed 的畫面貼入此處。

期望顯示：
```
com.example.inventory.ItemDaoTest > insertStudentIdItem_verifyStoredInDatabase PASSED
```
