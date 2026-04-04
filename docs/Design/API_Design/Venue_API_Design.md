# 場地與組織模組：模組 API 文件 (V1.1)

**日期：** 2026-04-04  
**基礎路徑：** `/api`  
**認證方式：** 採用公開路徑 `/api/public`，不需攜帶 Token。

## 一、 設計理念：為何使用 `/api/public` 與排除校驗？

本模組的所有 API 均設計於 `/api/public` 路徑下，主要基於以下軟體架構與使用者體驗考量：

1.  **資訊公開性與易用性**：根據產品需求（PRD），使用者在登入前應能瀏覽場地資訊、設備狀況與預約月曆，以決定是否進行預約。若將此類靜態資料設為私有，將大幅增加前端開發的複雜度與使用者的操作門檻。
2.  **效能優化 (Security Performance)**：在 `WebConfig.java` 中，我們將 `/api/public/**` 排除在 `MockAuthInterceptor` 攔截範圍之外。這樣可以讓這類 **Read-Heavy (高頻讀取)** 的請求跳過 Token 解析與身分驗證邏輯，減輕伺服器負擔並加速前端載入。
3.  **職責分離**：將「查看場地（公開）」與「提交預約（私有）」路徑隔離。只有涉及資料異動（如 `POST /bookings`）的行為才需要受 `UserContext` 保護。

---

## 二、 數據傳輸物件 (VO) 設計

依據 `function_tables.md` 的資料表結構，定義以下輸出物件：

### 1. UnitVO (管理單位資訊)
| 欄位名稱 | 類型 | 說明 |
| :--- | :--- | :--- |
| `id` | Long | 單位唯一識別碼 |
| `name` | String | 單位名稱 (如：學務處本部) |
| `code` | String | 單位代碼 (如：OSA) |

### 2. EquipmentVO (設備資訊)
| 欄位名稱 | 類型 | 說明 |
| :--- | :--- | :--- |
| `id` | Long | 設備唯一識別碼 |
| `name` | String | 設備名稱 (如：無線麥克風) |

### 3. VenueVO (場地詳細資訊)
| 欄位名稱 | 類型 | 說明 |
| :--- | :--- | :--- |
| `id` | Long | 場地唯一識別碼 |
| `unitId` | Long | 所屬單位 ID |
| `name` | String | 場地名稱 |
| `capacity` | Integer | 容納人數上限 |
| `description` | String | 場地介紹或規則 |
| `equipments` | List\<EquipmentVO\> | 該場地可供借用的設備清單 (對應 `Venue_Equipment_Map`) |

---

## 三、 API 終端點 (Endpoints)

### 1. 取得管理單位清單
- **方法：** `GET`
- **路徑：** `/public/units`
- **功能：** 讓使用者選擇管理單位。雖然資料表支援多單位，但 MVP 階段僅回傳啟用中的單位（如學務處）。
- **響應格式 (`Result<List<UnitVO>>`)**：
    ```json
    {
      "success": true,
      "message": "操作成功",
      "data": [
        { "id": 1, "name": "學務處本部", "code": "OSA" }
      ]
    }
    ```

### 2. 根據單位取得場地列表
- **方法：** `GET`
- **路徑：** `/public/venues`
- **功能：** 根據選定的單位 ID，過濾並列出該單位下屬的所有場地基本資訊。
- **查詢參數：** `unitId` (Long, 必填)。
- **響應格式 (`Result<List<VenueVO>>`)**：
    ```json
    {
      "success": true,
      "message": "操作成功",
      "data": [
        {
          "id": 101,
          "unitId": 1,
          "name": "會議室 A",
          "capacity": 20,
          "description": "配備投影機",
          "equipments": [
             { "id": 1, "name": "投影機" },
             { "id": 2, "name": "無線麥克風" }
          ]
        }
      ]
    }
    ```

### 3. 取得場地詳細資訊 (含設備清單)
- **方法：** `GET`
- **路徑：** `/public/venues/{id}`
- **功能：** 取得特定場地的詳細規格與可借用設備。這能讓前端在渲染預約表單時，正確顯示可選購的設備清單（`equipmentIds`）。
- **響應格式 (`Result<VenueVO>`)**：
    ```json
    {
      "success": true,
      "message": "操作成功",
      "data": {
        "id": 101,
        "unitId": 1,
        "name": "會議室 A",
        "capacity": 20,
        "description": "位於學務處二樓...",
        "equipments": [
          { "id": 1, "name": "投影機" },
          { "id": 2, "name": "無線麥克風" }
        ]
      }
    }
    ```

---

## 四、 錯誤處理機制 (Error Handling)

本模組遵循 `GlobalExceptionHandler.java` 之規範：

1.  **參數校驗失敗**：若查詢時缺少 `unitId` 或路徑參數格式錯誤，回傳由攔截器捕獲的詳細提示。
2.  **資料不存在**：若查詢的 `id` 在資料庫中找不到，`data` 將回傳 `null`，且建議在 `message` 中提示「找不到該場地資訊」。
3.  **伺服器運行異常**：所有未預期的錯誤均封裝為「伺服器內部錯誤，請稍後再試」，確保系統健壯性。