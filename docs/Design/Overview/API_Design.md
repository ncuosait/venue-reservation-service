# API 設計文檔 - 場地預約系統

**更新日期**：2026-04-04  
**版本號**：V2.0 (包含日曆功能)  
**狀態**：已實現，準備與前端串接

---

## 📋 API 簽名規範

### 統一回應格式 (Result<T>)

所有 API 回應都遵循統一格式：

```json
{
  "success": true,
  "message": "操作成功",
  "data": { ... }
}
```

**回應字段說明：**
- `success` (Boolean)：執行是否成功（true 成功，false 失敗）
- `message` (String)：操作結果描述，成功時為「操作成功」，失敗時為具體的錯誤原因
- `data` (Generic)：實際回傳的數據，失敗時為 null

---

## 🛠️ Module 1：場地與基礎數據查詢 API

**基路徑：** `/api/public`  
**功能：** 提供前端渲染場地選擇器和初始化數據

### 1.1 取得所有管理單位清單

- **端點：** `GET /api/public/units`
- **功能：** 取得系統中所有的管理單位
- **請求參數：** 無
- **成功回應 (200)：**
  ```json
  {
    "success": true,
    "message": "操作成功",
    "data": [
      {
        "id": 1,
        "name": "學務處本部",
        "code": "STUA"
      },
      {
        "id": 2,
        "name": "住宿服務組",
        "code": "HSD"
      }
    ]
  }
  ```

**回應字段說明：**
- `id` (Long)：單位唯一識別碼
- `name` (String)：單位名稱
- `code` (String)：單位代碼（對接 Portal 身分用）

### 1.2 根據單位取得場地清單

- **端點：** `GET /api/public/venues`
- **功能：** 根據選定的單位，查詢該單位下的所有場地
- **請求參數：**
  - `unitId` (Long，必填，Query)：單位 ID

- **成功回應 (200)：**
  ```json
  {
    "success": true,
    "message": "操作成功",
    "data": [
      {
        "id": 101,
        "unitId": 1,
        "name": "會議室 A",
        "capacity": 50,
        "description": "配備投影機與無線麥克風"
      },
      {
        "id": 102,
        "unitId": 1,
        "name": "會議室 B",
        "capacity": 30,
        "description": "配備白板與視訊會議系統"
      }
    ]
  }
  ```

**回應字段說明：**
- `id` (Long)：場地唯一識別碼
- `unitId` (Long)：所屬單位 ID
- `name` (String)：場地名稱
- `capacity` (Integer)：容納人數上限
- `description` (String)：場地介紹或借用規則說明

- **失敗回應 (400)：** 缺少必填參數 `unitId`
  ```json
  {
    "success": false,
    "message": "缺少必填參數：unitId",
    "data": null
  }
  ```

### 1.3 取得單一場地的詳細資訊

- **端點：** `GET /api/public/venues/{id}`
- **功能：** 取得單一場地的詳細資訊，包括可借用設備清單
- **路徑參數：**
  - `id` (Long，必填，Path)：場地 ID

- **成功回應 (200)：**
  ```json
  {
    "success": true,
    "message": "操作成功",
    "data": {
      "id": 101,
      "unitId": 1,
      "name": "會議室 A",
      "capacity": 50,
      "description": "配備投影機與無線麥克風"
    }
  }
  ```

- **失敗回應 (404)：** 場地不存在
  ```json
  {
    "success": false,
    "message": "場地不存在",
    "data": null
  }
  ```

---

## 🛠️ Module 2：預約操作 API

**基路徑：** `/api/bookings`  
**功能：** 處理預約申請的完整生命週期（提交、查詢、修改、撤回）

### 2.1 提交預約申請

- **端點：** `POST /api/bookings`
- **功能：** 提交新的預約申請，系統會自動檢查時段衝突
- **請求參數（Body）：**
  ```json
  {
    "venueId": 101,
    "bookingDate": "2026-04-06",
    "slots": [8, 9, 10],
    "purpose": "課程討論會議",
    "participantCount": 15,
    "contactInfo": {
      "name": "王小明",
      "email": "student@ncu.edu.tw",
      "phone": "0912345678"
    },
    "equipmentIds": [1, 2]
  }
  ```

**請求字段說明：**
- `venueId` (Long，必填)：要預約的場地 ID
- `bookingDate` (LocalDate，必填)：預約日期（ISO 8601 格式：YYYY-MM-DD）
- `slots` (List<Integer>，必填)：預約時段，0-23 表示 0 點到 23 點（例如 [8, 9] 表示 08:00-10:00）
- `purpose` (String，必填)：使用用途（255 字以內）
- `participantCount` (Integer，必填)：預估參與人數（至少 1 人）
- `contactInfo.name` (String，必填)：聯絡人姓名
- `contactInfo.email` (String，必填)：聯絡人電子郵件
- `contactInfo.phone` (String，必填)：聯絡人電話
- `equipmentIds` (List<Long>，可選)：需要借用的設備 ID 列表

- **成功回應 (200)：**
  ```json
  {
    "success": true,
    "message": "操作成功",
    "data": 501
  }
  ```

**回應字段說明：**
- `data` (Long)：新建立的預約案 ID

- **失敗回應 - 時段衝突 (200)：**
  ```json
  {
    "success": false,
    "message": "該時段已被其他已通過之申請佔用",
    "data": null
  }
  ```

- **失敗回應 - 參數驗證 (200)：**
  ```json
  {
    "success": false,
    "message": "預約日期不能是過去的時間",
    "data": null
  }
  ```

### 2.2 查看個人預約清單

- **端點：** `GET /api/bookings/my`
- **功能：** 查看當前登入用戶的所有預約申請清單
- **請求參數：** 無
- **成功回應 (200)：**
  ```json
  {
    "success": true,
    "message": "操作成功",
    "data": [
      {
        "id": 501,
        "venueName": "會議室 A",
        "bookingDate": "2026-04-06",
        "slots": [8, 9, 10],
        "status": 1,
        "createdAt": "2026-04-01T10:00:00"
      },
      {
        "id": 502,
        "venueName": "會議室 B",
        "bookingDate": "2026-04-07",
        "slots": [14, 15],
        "status": 2,
        "createdAt": "2026-04-02T11:30:00"
      }
    ]
  }
  ```

**回應字段說明：**
- `id` (Long)：預約案編號
- `venueName` (String)：場地名稱
- `bookingDate` (LocalDate)：預約日期
- `slots` (List<Integer>)：預約時段列表
- `status` (Integer)：預約狀態（0=撤回, 1=審核中, 2=已通過, 3=已拒絕）
- `createdAt` (LocalDateTime)：建立時間

### 2.3 修改預約申請

- **端點：** `PUT /api/bookings/{id}`
- **功能：** 修改未被核准或已核准的預約申請，修改後狀態重置為「審核中」
- **路徑參數：**
  - `id` (Long，必填，Path)：預約案 ID

- **請求參數（Body）：** 同提交預約申請（2.1）

- **成功回應 (200)：**
  ```json
  {
    "success": true,
    "message": "操作成功",
    "data": null
  }
  ```

- **失敗回應 - 無權限 (400)：**
  ```json
  {
    "success": false,
    "message": "無權限修改他人的預約申請",
    "data": null
  }
  ```

- **失敗回應 - 狀態不符 (400)：**
  ```json
  {
    "success": false,
    "message": "該預約申請已被拒絕或已撤回，無法修改",
    "data": null
  }
  ```

### 2.4 撤回預約申請

- **端點：** `PUT /api/bookings/{id}/withdraw`
- **功能：** 撤回預約申請（僅限「審核中」或「已通過」狀態可撤回）
- **路徑參數：**
  - `id` (Long，必填，Path)：預約案 ID

- **請求參數：** 無

- **成功回應 (200)：**
  ```json
  {
    "success": true,
    "message": "操作成功",
    "data": null
  }
  ```

- **失敗回應 - 無權限 (400)：**
  ```json
  {
    "success": false,
    "message": "無權限撤回他人的預約申請",
    "data": null
  }
  ```

- **失敗回應 - 狀態不符 (400)：**
  ```json
  {
    "success": false,
    "message": "已拒絕或已撤回之申請無法再次撤回",
    "data": null
  }
  ```

---

## 🛠️ Module 3：日曆視圖查詢 API（✨ 新增功能）

**基路徑：** `/api/bookings/calendar`  
**功能：** 提供三層級別（月/周/日）的日曆視圖，用戶可查看場地的已占用時段和自己的預約紀錄

### 3.1 月曆視圖

- **端點：** `GET /api/bookings/calendar/month`
- **功能：** 查看指定月份的日曆，標記每日是否有已占用時段和用戶預約
- **請求參數：**
  - `venueId` (Long，必填，Query)：場地 ID
  - `year` (Integer，必填，Query)：年份
  - `month` (Integer，必填，Query)：月份（1-12）

- **成功回應 (200)：**
  ```json
  {
    "success": true,
    "message": "操作成功",
    "data": {
      "year": 2026,
      "month": 4,
      "days": [
        {
          "date": "2026-04-01",
          "hasApprovedBooking": false,
          "hasUserBooking": false
        },
        {
          "date": "2026-04-06",
          "hasApprovedBooking": true,
          "hasUserBooking": false
        },
        {
          "date": "2026-04-08",
          "hasApprovedBooking": false,
          "hasUserBooking": true
        },
        {
          "date": "2026-04-09",
          "hasApprovedBooking": false,
          "hasUserBooking": true
        }
      ]
    }
  }
  ```

**回應字段說明：**
- `year` (Integer)：年份
- `month` (Integer)：月份
- `days` (List)：該月每日的摘要
  - `date` (String)：日期（ISO 8601 格式）
  - `hasApprovedBooking` (Boolean)：該日是否有已通過審核的預約
  - `hasUserBooking` (Boolean)：該日是否有用戶自己的預約

- **失敗回應 - 參數無效 (400)：**
  ```json
  {
    "success": false,
    "message": "年份或月份格式不正確",
    "data": null
  }
  ```

### 3.2 周曆視圖

- **端點：** `GET /api/bookings/calendar/week`
- **功能：** 查看指定周的詳細時段占用情況（包含時段級別的細節）
- **請求參數：**
  - `venueId` (Long，必填，Query)：場地 ID
  - `date` (LocalDate，必填，Query)：周開始日期（必須為周一，ISO 8601 格式）

- **成功回應 (200)：**
  ```json
  {
    "success": true,
    "message": "操作成功",
    "data": {
      "weekStart": "2026-04-06",
      "weekEnd": "2026-04-12",
      "days": [
        {
          "date": "2026-04-06",
          "dayOfWeek": "星期一",
          "approvedSlots": [9, 10, 11],
          "userSlots": [14, 15, 16]
        },
        {
          "date": "2026-04-07",
          "dayOfWeek": "星期二",
          "approvedSlots": [8, 9, 10],
          "userSlots": []
        }
      ]
    }
  }
  ```

**回應字段說明：**
- `weekStart` (String)：周開始日期
- `weekEnd` (String)：周結束日期
- `days` (List)：周內每日詳細資訊
  - `date` (String)：日期
  - `dayOfWeek` (String)：星期幾（中文表示：星期一至星期日）
  - `approvedSlots` (List<Integer>)：該日已通過審核的預約時段（0-23 升序排列）
  - `userSlots` (List<Integer>)：該日用戶的預約時段（0-23 升序排列）

- **失敗回應 - 周開始日期非周一 (400)：**
  ```json
  {
    "success": false,
    "message": "周開始日期必須為周一",
    "data": null
  }
  ```

### 3.3 日曆視圖

- **端點：** `GET /api/bookings/calendar/day`
- **功能：** 查看指定日期的最詳細信息，包括時段占用情況和用戶的預約詳細清單
- **請求參數：**
  - `venueId` (Long，必填，Query)：場地 ID
  - `date` (LocalDate，必填，Query)：查詢日期（ISO 8601 格式）

- **成功回應 (200)：**
  ```json
  {
    "success": true,
    "message": "操作成功",
    "data": {
      "venueId": 101,
      "venueName": "場地 101",
      "date": "2026-04-06",
      "dayOfWeek": "星期一",
      "approvedSlots": [9, 10, 11, 14],
      "userSlots": [15, 16, 17],
      "userBookingDetails": [
        {
          "bookingId": 501,
          "slots": [15, 16, 17],
          "status": 1,
          "purpose": "課程講座",
          "createdAt": "2026-04-01T10:00:00"
        }
      ]
    }
  }
  ```

**回應字段說明：**
- `venueId` (Long)：場地 ID
- `venueName` (String)：場地名稱
- `date` (String)：日期
- `dayOfWeek` (String)：星期幾（中文表示）
- `approvedSlots` (List<Integer>)：該日已通過審核的預約時段（0-23 升序排列，去重）
- `userSlots` (List<Integer>)：該日用戶的預約時段（0-23 升序排列，去重）
- `userBookingDetails` (List)：用戶在該日的所有預約詳情
  - `bookingId` (Long)：預約案編號
  - `slots` (List<Integer>)：該筆預約的時段列表
  - `status` (Integer)：預約狀態（0=撤回, 1=審核中, 2=已通過, 3=已拒絕）
  - `purpose` (String)：使用用途
  - `createdAt` (LocalDateTime)：預約建立時間

- **失敗回應 - 參數無效 (400)：**
  ```json
  {
    "success": false,
    "message": "日期不可為空",
    "data": null
  }
  ```

---

## 📊 API 統計表

| Module | 功能名稱 | HTTP 方法 | 端點 | 狀態 |
|:---|:---|:---|:---|:---|
| **場地與基礎數據** | 取得所有管理單位 | GET | `/api/public/units` | ✅ 已實現 |
| | 根據單位取得場地清單 | GET | `/api/public/venues` | ✅ 已實現 |
| | 取得單一場地詳細資訊 | GET | `/api/public/venues/{id}` | ✅ 已實現 |
| **預約操作** | 提交預約申請 | POST | `/api/bookings` | ✅ 已實現 |
| | 查看個人預約清單 | GET | `/api/bookings/my` | ✅ 已實現 |
| | 修改預約申請 | PUT | `/api/bookings/{id}` | ✅ 已實現 |
| | 撤回預約申請 | PUT | `/api/bookings/{id}/withdraw` | ✅ 已實現 |
| **日曆視圖**（✨新增） | 月曆視圖 | GET | `/api/bookings/calendar/month` | ✅ 已實現 |
| | 周曆視圖 | GET | `/api/bookings/calendar/week` | ✅ 已實現 |
| | 日曆視圖 | GET | `/api/bookings/calendar/day` | ✅ 已實現 |

**總計：10 個 API 端點**

---

## 🔐 認證與授權

### 認證方式

目前系統使用 **Mock 認證機制**（`MockAuthInterceptor`）：

- 所有請求會自動設置一個模擬的用戶 ID（學工號）到 `UserContext`
- 生產環境應使用 **OAuth 2.0** 或 **JWT Token** 進行認證

### 用戶隔離原則

- **個人預約操作**：只能修改和撤回自己的預約（通過 `UserContext.getUser().getUserId()` 驗證）
- **日曆視圖查詢**：用戶只能看到自己的預約紀錄，已占用時段對所有用戶可見

---

## 🌐 跨域設置（CORS）

根據系統設計，前端應配置跨域請求：

```javascript
// 前端 CORS 配置範例
fetch('/api/bookings/calendar/month', {
  method: 'GET',
  credentials: 'include',  // 包含認證資訊
  headers: {
    'Content-Type': 'application/json',
  }
})
```

後端已配置 CORS，詳見 `WebConfig.java`

---

## ⚠️ 常見錯誤與排查

| 錯誤信息 | 原因 | 解決方案 |
|:---|:---|:---|
| 「缺少必填參數：unitId」 | Query 參數缺失 | 確認 URL 包含 `?unitId=1` |
| 「場地不存在」 | 查詢的場地 ID 不存在 | 先調用 `/api/public/venues` 獲取有效 ID |
| 「該時段已被其他已通過之申請佔用」 | 時段衝突 | 調用日曆 API 查看占用情況，選擇可用時段 |
| 「無權限修改他人的預約申請」 | 修改他人預約 | 確認登入用戶與預約所有者相同 |
| 「周開始日期必須為周一」 | 周曆查詢的日期不是周一 | 確保傳入的日期是周一（ISO 8601 格式） |

---

## 📝 使用示例

### 完整的預約流程示例

```
1. 用戶選擇單位
   GET /api/public/units
   → 獲取所有單位列表

2. 用戶根據單位選擇場地
   GET /api/public/venues?unitId=1
   → 獲取學務處的所有場地

3. 用戶查看場地詳細資訊
   GET /api/public/venues/101
   → 獲取會議室 A 的詳細資訊

4. 用戶查看場地的月曆（了解占用情況）
   GET /api/bookings/calendar/month?venueId=101&year=2026&month=4
   → 查看 4 月份的月曆視圖

5. 用戶查看周曆或日曆以查看詳細時段
   GET /api/bookings/calendar/week?venueId=101&date=2026-04-06
   或
   GET /api/bookings/calendar/day?venueId=101&date=2026-04-06
   → 查看詳細的時段占用情況

6. 用戶提交預約申請
   POST /api/bookings
   {
     "venueId": 101,
     "bookingDate": "2026-04-06",
     "slots": [14, 15, 16],
     ...
   }
   → 提交預約申請

7. 用戶查看自己的預約清單
   GET /api/bookings/my
   → 查看所有個人預約

8. 用戶修改或撤回預約
   PUT /api/bookings/501
   或
   PUT /api/bookings/501/withdraw
   → 修改或撤回預約
```

---

## 📱 前端集成建議

### 1. 基礎 URL 配置

```javascript
const API_BASE_URL = 'http://localhost:8080/api';
```

### 2. HTTP 客戶端設置

確保使用支持以下特性的 HTTP 客戶端（如 axios）：
- 自動 JSON 序列化/反序列化
- 攜帶 Credentials（用於認證）
- 統一的錯誤處理

### 3. 響應處理

```javascript
// 統一處理 Result<T> 格式
const handleResponse = (response) => {
  if (response.success) {
    return response.data;
  } else {
    throw new Error(response.message);
  }
};
```

### 4. 日期格式

所有日期參數使用 **ISO 8601 格式**：
- 日期：`YYYY-MM-DD`（例如：`2026-04-06`）
- 日期時間：`YYYY-MM-DDTHH:mm:ss`（例如：`2026-04-01T10:00:00`）

### 5. 時段表示

時段使用 **0-23 的整數列表** 表示小時：
- `[8, 9, 10]` = 08:00 ~ 11:00（3 個小時）
- `[0, 1, 2, ..., 23]` = 全天 24 小時

---

## ✅ 準備事項檢查清單

在開始前端集成前，請確認：

- [ ] 後端服務已啟動（localhost:8080）
- [ ] 所有 API 端點已測試通過（見測試命令）
- [ ] 數據庫已正確初始化（含測試數據）
- [ ] CORS 跨域設置已開啟
- [ ] 前端開發環境已配置 API 代理或 CORS 處理
- [ ] 團隊成員已了解本文檔中的 API 規範

---

## 📞 技術支援

如有任何問題，請參考：
- 設計文檔：`docs/Design/Module_Design/`
- 代碼規範：`.github/skills/enforcing-code-standard/`
- 快速開發指南：`docs/dev-process/Booking_Module/BOOKING_CALENDAR_FEATURE_QUICK_GUIDE.md`

**最後更新日期**：2026-04-04  
**版本**：V2.0 - 包含日曆功能

## 🛠️ 第一階段：場地與基礎數據 API (Metadata & Availability)

這部分的設計思想在於提供前端渲染「場地月曆」所需的靜態與動態數據。

### 1. 取得管理單位列表

- **端點：** `GET /api/units`
- **功能：** 讓使用者選擇要預約哪一個單位（如：學務處、住服組）。
- **成功回傳 (200 OK)：**
  ```json
  {
    "success": true,
    "message": "操作成功",
    "data": [
      { "id": 1, "name": "學務處本部", "code": "OSA" },
      { "id": 2, "name": "住宿服務組", "code": "HSD" }
    ]
  }
  ```

### 2. 根據單位取得場地列表

- **端點：** `GET /api/venues`
- **參數：** `unitId` (Query, Long, 必填)
- **成功回傳 (200 OK)：**
  ```json
  {
    "success": true,
    "message": "操作成功",
    "data": [
      {
        "id": 101,
        "name": "會議室 A",
        "capacity": 20,
        "description": "配備投影機與無線麥克風"
      }
    ]
  }
  ```
- **失敗回傳 (400 Bad Request - 缺少參數)：**
  ```json
  {
    "success": false,
    "message": "Required parameter 'unitId' is not present",
    "data": null
  }
  ```

### 3. 取得場地月曆佔用狀態 (關鍵核心)

- **端點：** `GET /api/venues/{id}/availability`
- **路徑參數：** `id` (場地 ID)
- **查詢參數：** `month` (String, 格式 `YYYY-MM`, 必填)
- **設計說明：** 後端會過濾出該月份所有 **狀態為 APPROVED** 的申請案，並將同日期的時段進行位元 `OR` 運算，回傳當天總佔用遮罩。
- **成功回傳 (200 OK)：**
  ```json
  {
    "success": true,
    "message": "操作成功",
    "data": {
      "2026-03-01": 0, // 0 代表全天可借
      "2026-03-02": 768, // 08:00-10:00 已被佔用 (2^8 + 2^9)
      "2026-03-03": 16777215 // 16777215 = 2^24 - 1 (全天已被佔用)
    }
  }
  ```
- **失敗回傳 (404 Not Found - 場地不存在)：**
  ```json
  {
    "success": false,
    "message": "找不到該場地資訊",
    "data": null
  }
  ```

---

## 🛠️ 第二階段：使用者預約操作 API (Booking Operations)

這部分處理申請的生命週期，包含提交、查詢與撤回。

### 4. 提交預約申請

- **端點：** `POST /api/bookings`
- **接收 JSON (DTO)：**
  ```json
  {
    "venueId": 101,
    "bookingDate": "2026-03-27",
    "slots": [8, 9, 10],
    "purpose": "專案小組週會",
    "participantCount": 5,
    "contactInfo": {
      "name": "王小明",
      "email": "student@ncu.edu.tw",
      "phone": "0912345678"
    },
    "equipmentNames": ["麥克風", "白板筆"]
  }
  ```
- **衝突檢查成功 (200 OK)：**
  ```json
  {
    "success": true,
    "message": "申請已送出，請靜候管理員審核",
    "data": { "bookingId": 501 }
  }
  ```
- **時段衝突失敗 (200 OK - 業務錯誤)：**
  ```json
  {
    "success": false,
    "message": "該時段已被其他已通過之申請佔用",
    "data": null
  }
  ```

### 5. 查看個人申請清單

- **端點：** `GET /api/my/bookings`
- **成功回傳 (200 OK)：**
  ```json
  {
    "success": true,
    "message": "操作成功",
    "data": [
      {
        "id": 501,
        "venueName": "會議室 A",
        "bookingDate": "2026-03-27",
        "slots": [8, 9, 10],
        "status": 1, // 1: 審核中
        "createdAt": "2026-03-20 10:00:00"
      }
    ]
  }
  ```

### 6. 撤回申請

- **端點：** `PUT /api/bookings/{id}/withdraw`
- **說明：** 僅限狀態為「審核中」或「已通過」的案件可撤回。
- **成功回傳 (200 OK)：**
  ```json
  {
    "success": true,
    "message": "申請已撤回",
    "data": null
  }
  ```

---

## 🛠️ 第三階段：管理員審核與稽核 API (Admin & Audit)

### 7. 管理員視角：月曆詳細清單

- **端點：** `GET /api/admin/bookings/calendar`
- **功能：** 供管理員在月曆介面查看所屬單位的所有申請案（包含 Pending, Approved, Rejected）。
- **查詢參數：** `unitId` (Long, 必填), `month` (String, `YYYY-MM`, 必填)
- **設計說明：** 不同於前台只回傳遮罩（Mask），此 API 回傳詳細列表，讓管理員點擊月曆格子時能看到是誰借的、要做什麼。
- **成功回傳 (200 OK)：**
  ```json
  {
    "success": true,
    "message": "操作成功",
    "data": [
      {
        "id": 501,
        "userName": "張小明",
        "venueName": "會議室 A",
        "bookingDate": "2026-03-27",
        "slots": [8, 9, 10],
        "status": 1,
        "purpose": "資管系專題討論"
      }
    ]
  }
  ```

### 8. 審核申請案 (通過或拒絕)

- **端點：** `POST /api/admin/bookings/{id}/audit`
- **路徑參數：** `id` (申請案 ID)
- **接收 JSON (DTO)：**
  ```json
  {
    "status": 2, // 2: APPROVED, 3: REJECTED
    "adminRemark": "准予借用，請注意環境整潔"
  }
  ```
- **設計說明：** 後端在更新狀態的同時，必須觸發發送郵件通知，並將此審核動作紀錄至 `Audit_Logs`。
- **成功回傳 (200 OK)：**
  ```json
  {
    "success": true,
    "message": "審核完成",
    "data": null
  }
  ```

### 9. 管理員強制修改預約 (變更時段或資訊)

- **端點：** `PUT /api/admin/bookings/{id}`
- **功能：** 管理員可直接修改任何狀態的申請，例如協助學生微調時段。
- **接收 JSON (DTO)：**
  ```json
  {
    "bookingDate": "2026-03-27",
    "slots": [10, 11],
    "venueId": 101
  }
  ```
- **設計說明：** 這是最高權限操作，**必須**強制記錄修改前與修改後的 `old_data` 與 `new_data`。
- **成功回傳 (200 OK)：**
  ```json
  {
    "success": true,
    "message": "修改成功並已存入稽核日誌",
    "data": null
  }
  ```

### 10. 查詢稽核日誌 (Audit Logs)

- **端點：** `GET /api/admin/audit-logs`
- **功能：** 供系統管理員查詢所有管理動作紀錄。
- **參數：** `bookingId` (Query, 可選), `page` (Query, 預設 1)
- **成功回傳 (200 OK)：**
  ```json
  {
    "success": true,
    "message": "操作成功",
    "data": {
      "list": [
        {
          "id": 99,
          "operatorName": "學務處管理員-王大同",
          "action": "UPDATE_STATUS",
          "oldValue": "PENDING",
          "newValue": "APPROVED",
          "createdAt": "2026-03-27 10:10:00"
        }
      ],
      "total": 1
    }
  }
  ```

### 11. 管理員白名單管理 (Super Admin Only)

- **端點：** `POST /api/admin/white-list`
- **功能：** 增加或移除擁有管理權限的用戶。
- **接收 JSON：**
  ```json
  {
    "userId": "staff001",
    "unitId": 1,
    "action": "ADD" // ADD 或 REMOVE
  }
  ```
