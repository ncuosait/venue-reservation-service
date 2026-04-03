# 預約核心引擎：模組 API 文件 (V1.0)

**日期：** 2026-04-03
**基礎路徑：** `/api`
**認證方式：** 於 Header 攜帶 `Authorization: mock-token-123` (MVP 階段使用)

## 一、 數據傳輸物件 (DTO/VO) 設計

在進行 API 呼叫前，請參考以下結構定義：

### 1. BookingRequestDTO (預約申請提交/修改)
| 欄位名稱 | 類型 | 必填 | 說明 |
| :--- | :--- | :--- | :--- |
| `venueId` | Long | 是 | 場地唯一識別碼 |
| `bookingDate` | LocalDate | 是 | 預約日期 (格式：YYYY-MM-DD) |
| `slots` | List\<Integer\> | 是 | 預約時段列表 (0-23，代表整點區間) |
| `purpose` | String | 是 | 使用用途 (最大 255 字) |
| `participantCount` | Integer | 是 | 預計使用人數 |
| `contactInfo` | ContactInfoDTO | 是 | 聯絡人詳細資訊 |
| `equipmentIds` | List\<Long\> | 否 | 借用設備 ID 列表 |

### 2. ContactInfoDTO (聯絡資訊)
| 欄位名稱 | 類型 | 說明 |
| :--- | :--- | :--- |
| `name` | String | 聯絡人姓名 |
| `email` | String | 聯絡人電子郵件 |
| `phone` | String | 聯絡電話 |

### 3. BookingVO (個人申請列表顯示)
| 欄位名稱 | 類型 | 說明 |
| :--- | :--- | :--- |
| `id` | Long | 申請案編號 |
| `venueName` | String | 場地名稱 |
| `bookingDate` | LocalDate | 預約日期 |
| `slots` | List\<Integer\> | 轉換後的時段清單 |
| `status` | Integer | 狀態 (0:撤回, 1:審核中, 2:通過, 3:拒絕) |
| `createdAt` | LocalDateTime | 申請時間 |

---

## 二、 API 終端點 (Endpoints)

### 1. 提交預約申請
- **方法：** `POST`
- **路徑：** `/bookings`
- **功能：** 建立一筆新的預約申請，初始狀態為「審核中」。
- **請求範例：**
  ```json
  {
    "venueId": 101,
    "bookingDate": "2026-04-10",
    "slots": [8, 9],
    "purpose": "專案討論",
    "participantCount": 5,
    "contactInfo": {
      "name": "王小明",
      "email": "xm@ncu.edu.tw",
      "phone": "0912345678"
    },
    "equipmentIds": [1, 2]
  }
  ```
- **響應格式 (`Result<Long>`)：**
    - **成功 (200 OK)：**
      ```json
      { "success": true, "message": "操作成功", "data": 501 }
      ```
    - **業務錯誤 (200 OK)：**
      ```json
      { "success": false, "message": "該時段已被其他已通過之申請佔用", "data": null }
      ```

### 2. 查看個人預約清單
- **方法：** `GET`
- **路徑：** `/my/bookings`
- **功能：** 根據當前登入者 ID 取得其所有歷史申請記錄。
- **響應格式 (`Result<List<BookingVO>>`)：**
    - **成功 (200 OK)：**
      ```json
      {
        "success": true,
        "message": "操作成功",
        "data": [
          {
            "id": 501,
            "venueName": "會議室 A",
            "bookingDate": "2026-04-10",
            "slots": [8, 9],
            "status": 1,
            "createdAt": "2026-04-03T10:00:00"
          }
        ]
      }
      ```

### 3. 撤回預約申請
- **方法：** `PUT`
- **路徑：** `/bookings/{id}/withdraw`
- **功能：** 申請人主動撤回申請。僅限狀態為「審核中」或「已通過」的案件。
- **響應格式 (`Result<Void>`)：**
    - **成功 (200 OK)：**
      ```json
      { "success": true, "message": "操作成功", "data": null }
      ```
    - **錯誤範例：**
      ```json
      { "success": false, "message": "已拒絕之申請無法撤回", "data": null }
      ```

### 4. 修改預約申請
- **方法：** `PUT`
- **路徑：** `/bookings/{id}`
- **功能：** 修改尚未被核准或已核准的申請內容。修改後狀態將重置為「審核中」。
- **參數：** 同 `POST` 請求之 `BookingRequestDTO`。
- **響應格式 (`Result<Void>`)：**
    - **成功 (200 OK)：**
      ```json
      { "success": true, "message": "操作成功", "data": null }
      ```

---

## 三、 錯誤處理機制 (Error Handling)

當系統發生異常時，會由 `GlobalExceptionHandler` 捕獲並轉化為 `Result.error()` 格式：

1.  **參數校驗失敗 (`400 Bad Request`)**：若 `BookingRequestDTO` 欄位不符規範（如人數為負數），回傳 DTO 定義的錯誤訊息。
2.  **時段衝突**：業務邏輯拋出 `RuntimeException`，顯示「該時段已被其他已通過之申請佔用」。
3.  **未預期錯誤**：回傳「伺服器內部錯誤，請稍後再試」。
