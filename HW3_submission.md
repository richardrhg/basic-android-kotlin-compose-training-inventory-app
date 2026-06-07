# HW3 存貨資料庫的測試案例 — 繳交文件

**姓名：** 周君諺  
**學號：** B11330218  
**課程：** 行動裝置程式設計

---

## 1. GitHub Repository

本作業 fork 自 Google 官方範例專案，改動後推送至本人帳號下的 repository：

**https://github.com/richardrhg/basic-android-kotlin-compose-training-inventory-app**

> 此 repo 在本人帳號 `richardrhg` 底下，所有測試程式碼皆為本人新增的 commit，與 Google 原始專案分離。

**已將以下助教帳號加入為 Collaborator**（Settings → Collaborators → Add people）：

- `cookiecatowo`
- `penpenpenguin`

---

## 2. 三項測試對應的檔案與測試方法

| 要求 | 測試方法 | 檔案 | 測試類型 |
|---|---|---|---|
| 要求 1 | `insertDuplicateId_keepsOriginalItem` | `app/src/androidTest/.../ItemDaoTest.kt` | Instrumented（需模擬器） |
| 要求 2 | `reduceQuantityByOne_whenQuantityGreaterThanZero_updatesItemWithDecrementedQuantity`<br>`reduceQuantityByOne_whenQuantityIsZero_doesNotCallUpdateItem`<br>`deleteItem_callsRepositoryDeleteItem` | `app/src/test/.../ItemDetailsViewModelTest.kt` | Local Unit（純 JVM，不需模擬器） |
| 要求 3 | `insertStudentIdItem_verifyStoredInDatabase` | `app/src/androidTest/.../ItemDaoTest.kt` | Instrumented（需模擬器） |

### 各測試的驗證邏輯

**要求 1 — 重複 ID 插入只保留第一筆**  
先插入 `item1`（id=1, Apples），再插入一筆相同 id=1 但內容不同的資料。Room 的 `@Insert` 預設策略會「靜默忽略」第二筆（不丟例外），因此測試驗證：資料庫只剩 1 筆，且內容仍為第一筆的 Apples。

**要求 2 — ItemDetailsViewModel 的三個行為**  
使用自製的 `FakeItemsRepository`（以 `MutableStateFlow` 模擬 Room 的 Flow）取代真實 Repository，避免依賴資料庫。三個測試分別驗證：
- 數量 > 0 時，`reduceQuantityByOne()` 會呼叫 `updateItem()` 且數量減 1。
- 數量 = 0 時，`reduceQuantityByOne()` 不會呼叫 `updateItem()`。
- `deleteItem()` 會正確呼叫 `itemsRepository.deleteItem()`。

**要求 3 — 自動新增學號資料並驗證**  
自動插入一筆 `name = "B11330218"`、`price = 100.0`、`quantity = 10` 的資料，再從資料庫查回，驗證三個欄位皆正確儲存（確認新增成功）。

---

## 3. 使用的 AI 工具與 Prompt

**使用工具：** Claude（Anthropic）— Cowork 模式

**下達的 Prompt（摘要）：**
1. 提供作業三大需求規格（重複 ID、ViewModel 三項行為、學號自動新增）。
2. 提供參考專案連結（Google Inventory App）。
3. 提供本人學號 `B11330218`，要求用於要求 3 的測試資料。
4. 請 AI 先分析原始程式碼（`ItemDao.kt`、`ItemDetailsViewModel.kt`、`ItemsRepository` 介面），再產生對應測試。

**AI 產生後本人所做的修正：**
- 確認 `SavedStateHandle` 的 key（`"itemId"`）與原始碼 `ItemDetailsDestination.itemIdArg` 一致，否則 ViewModel 取不到 id。
- 在 ViewModel 測試中加入 `backgroundScope.launch { viewModel.uiState.collect {} }`，因為 `uiState` 是 `WhileSubscribed` 的 StateFlow，必須有訂閱者才會真正計算與更新狀態。
- 要求 3 的測試改用 `id = 0` 讓 Room `autoGenerate` 自動產生主鍵，避免與其他測試的固定 id 衝突；驗證時改以 `name` 查找，避開自動產生 id 不確定的問題。

---

## 4. 過程中遇到的問題與解決

**問題：ItemDetailsViewModelTest 出現 14 個編譯錯誤，且 IDE 沒有出現 Run 按鈕。**

- **原因：** 專案的 `test`（本機單元測試）source set 缺少 JUnit 本體相依。原專案只在 `androidTest` 設定了 JUnit，`test` 區塊僅有 `kotlinx-coroutines-test`。因此 `import org.junit.Test`、`org.junit.Assert` 等全部找不到套件，IDE 不把該檔視為可執行的測試類別，自然不顯示 Run。
- **解決：** 在 `app/build.gradle.kts` 的 dependencies 中補上：
  ```kotlin
  testImplementation("junit:junit:4.13.2")
  ```
  再執行 **Gradle Sync**（工具列大象圖示 / 編輯器上方 Sync Now）。Sync 完成後紅字消失，class 名稱旁出現綠色 Run 三角，測試即可執行。

**第三個測試的版本問題（作業特別提及）**

要求 3 屬於 Instrumented Test，必須在模擬器/實機上執行，對 Android SDK、Room、模擬器 API 版本的相容性較敏感。助教下載後若環境版本不同，可能無法在其機器上順利執行，因此附上本人實機/模擬器跑過並通過的截圖（見第 6 節）作為佐證。本人執行環境為 **Pixel 9 Pro XL (AVD)**，測試全部通過。

---

## 5. 如何執行測試（請助教依此執行）

### 要求 2（ItemDetailsViewModelTest）— 純 JVM，不需模擬器

Android Studio：開啟 `ItemDetailsViewModelTest.kt` → 右鍵類別名 → **Run 'ItemDetailsViewModelTest'**

指令列：
```bash
./gradlew test --tests "com.example.inventory.ItemDetailsViewModelTest"
```
預期結果：**3 tests passed**。

### 要求 1 & 3（ItemDaoTest）— 需先啟動模擬器或接上實機

Android Studio：先啟動一台 AVD → 開啟 `ItemDaoTest.kt` → 右鍵類別名 → **Run 'ItemDaoTest'**（會執行全部 7 個方法）。

或單獨執行：
- 要求 1：右鍵 `insertDuplicateId_keepsOriginalItem` → Run
- 要求 3：右鍵 `insertStudentIdItem_verifyStoredInDatabase` → Run

指令列：
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.inventory.ItemDaoTest
```
預期結果：**7 tests passed**（含原有 5 個 + 要求 1、要求 3）。

> 注意：Instrumented Test 不會跳出 App 畫面，它只在背景的模擬器環境操作資料庫，Run 面板顯示綠色 PASSED 即為成功。

---

## 6. 執行結果截圖

### 要求 1 & 3 — ItemDaoTest（7 passed）

> 請貼上 Android Studio 執行 `ItemDaoTest` 後，Run 面板顯示 7 passed、且清單中包含
> `insertDuplicateId_keepsOriginalItem` 與 `insertStudentIdItem_verifyStoredInDatabase` 的截圖。

```
Starting 7 tests on Pixel_9_Pro_XL(AVD)
Finished 7 tests on Pixel_9_Pro_XL(AVD)
BUILD SUCCESSFUL
com.example.inventory.ItemDaoTest > insertStudentIdItem_verifyStoredInDatabase PASSED
```

### 要求 2 — ItemDetailsViewModelTest（3 passed）

> 請貼上 Android Studio 執行 `ItemDetailsViewModelTest` 後，Run 面板顯示 3 tests passed 的截圖。

```
Executing tasks: [:app:testDebugUnitTest --tests com.example.inventory.ItemDetailsViewModelTest]
Test Results — 3 tests passed
```
