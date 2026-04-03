# Booking 預約模組 - 快速開始指南

## 📌 概述

Booking Module 提供完整的場地預約管理功能，包括建立、查詢、修改和撤回預約申請。所有操作均基於**樂觀鎖**並行控制和**位元遮罩**時段管理。

## 🚀 快速開始

### 環境要求
- Java 11+
- Spring Boot 3.x
- MyBatis
- PostgreSQL/MySQL 8.0+

### 啟動應用
```bash
# 編譯
mvn clean install

# 運行
mvn spring-boot:run

# 或通過 IDE 直接運行 VenueReservationSystemApplication
```

## 📋 API 端點概覽

### 1️⃣ 提交預約申請

**請求**
```http
POST /api/bookings
Content-Type: application/json
Authorization: mock-token-123

{
  "venueId": 101,
  "bookingDate": "2026-04-10",
  "slots": [8, 9, 10],
  "purpose": "產品討論會",
  "participantCount": 6,
  "contactInfo": {
    "name": "李明華",
    "email": "lmh@ncu.edu.tw",
    "phone": "0916666666"
  },
  "equipmentIds": [1, 2]
}
```

**回應**
```json
{
  "success": true,
  "message": "操作成功",
  "data": 501
}
```

**狀態碼**
| 情況 | HTTP | 說明 |
| :--- | :--- | :--- |
| 成功建立 | 200 | data: 新預約 ID |
| 時段衝突 | 200 | success: false，訊息說明原因 |
| 參數不合法 | 400 | 欄位驗證失敗 |
| 伺服器錯誤 | 500 | 系統異常 |

### 2️⃣ 查詢個人申請清單

**請求**
```http
GET /api/bookings/my
Authorization: mock-token-123
```

**回應**
```json
{
  "success": true,
  "message": "操作成功",
  "data": [
    {
      "id": 501,
      "venueName": "會議室 A",
      "bookingDate": "2026-04-10",
      "slots": [8, 9, 10],
      "status": 1,
      "createdAt": "2026-04-03T10:15:30"
    },
    {
      "id": 502,
      "venueName": "會議室 B",
      "bookingDate": "2026-04-11",
      "slots": [14, 15],
      "status": 2,
      "createdAt": "2026-04-02T14:22:15"
    }
  ]
}
```

**狀態代碼參考**
| 狀態碼 | 說明 |
| :--- | :--- |
| 0 | 已撤回 |
| 1 | 審核中 |
| 2 | 已通過 |
| 3 | 已拒絕 |

### 3️⃣ 修改預約申請

**請求**
```http
PUT /api/bookings/501
Content-Type: application/json
Authorization: mock-token-123

{
  "venueId": 102,
  "bookingDate": "2026-04-11",
  "slots": [14, 15, 16],
  "purpose": "修改後的用途描述",
  "participantCount": 8,
  "contactInfo": {
    "name": "李明華",
    "email": "lmh@ncu.edu.tw",
    "phone": "0916666666"
  },
  "equipmentIds": [1]
}
```

**回應**
```json
{
  "success": true,
  "message": "操作成功",
  "data": null
}
```

**重要說明**
- 修改後狀態自動重置為「審核中」(1)
- 版本號 (version) 自動遞增
- 原 `equipmentIds` 會被新的清單取代

### 4️⃣ 撤回預約申請

**請求**
```http
PUT /api/bookings/501/withdraw
Authorization: mock-token-123
```

**回應**
```json
{
  "success": true,
  "message": "操作成功",
  "data": null
}
```

**限制條件**
- 僅「審核中」(1) 或「已通過」(2) 的預約可撤回
- 撤回後狀態變為「已撤回」(0)
- 不可再次修改已撤回的預約

## 🔍 常見問題

### Q: 什麼是「時段」(slots)?
**A:** 預約系統將一天分為 24 小時段 (0-23，代表整點區間)：
- 0 = 00:00-01:00
- 8 = 08:00-09:00
- 23 = 23:00-24:00

例如 `[8, 9, 10]` 表示 08:00-11:00 三小時連續預約。

### Q: 如何判定時段是否衝突?
**A:** 系統使用 24-bit 位元遮罩與位元與運算：
```
slots [8, 9]      → 遮罩 0000000000000011 0000000000 (bit 8,9 設為 1)
slots [9, 10]     → 遮罩 0000000000000110 0000000000 (bit 9,10 設為 1)
位元與運算結果    → 0000000000000010 0000000000 (bit 9 重疊，非 0)
結論：有衝突 ❌
```

### Q: 樂觀鎖是什麼?
**A:** 每筆預約記錄包含 `version` 欄位。修改或撤回時：
1. 讀取現有 version (例如 v=1)
2. 執行更新時檢查 version 是否仍為 1
3. 若他人已修改 (version > 1)，更新失敗
4. 成功更新後 version 遞增 (v=2)

**優點**：無鎖高效併發控制，避免 Race Condition。

### Q: ContactInfo 如何存儲?
**A:** 聯絡人資訊序列化為 JSON 字串存入資料庫：
```json
{
  "name": "李明華",
  "email": "lmh@ncu.edu.tw",
  "phone": "0916666666"
}
```

Service 自動進行 JSON 序列化/反序列化，對外呼叫者無需關心。

### Q: equipmentIds 是否必填?
**A:** 否。`equipmentIds` 為可選欄位：
- 提供時：新建或修改預約時會關聯設備
- 不提供 (null/空陣列)：不借用任何設備

### Q: 為什麼修改預約後狀態重置為「審核中」?
**A:** 安全考量。修改預約的核心資訊（場地、時段、人數）後，需重新經過審核流程確認，不應直接保持「已通過」狀態。

### Q: 預約建立後無法立即修改設備清單嗎?
**A:** 可以。修改預約時提供新的 `equipmentIds` 清單，系統會自動關聯。舊的設備關聯會被新清單取代。

## 🔐 認證與授權

### Mock 認證 (MVP 階段)
目前系統使用簡化的 Mock 認證機制：

**攔截器：MockAuthInterceptor**
```java
// 檢查 Header 中的 Authorization 欄位
if ("mock-token-123".equals(authHeader)) {
    // 從 Token 中提取用戶資訊並設置到 UserContext ThreadLocal
    UserContext.setUser(mockUser);
}
```

**操作前驗證**
每個 Service 方法均驗證當前操作是否由預約擁有者執行：
```java
String userId = UserContext.getUser().getUserId();
if (!booking.getUserId().equals(userId)) {
    throw new RuntimeException("無權限修改他人的預約申請");
}
```

## 📊 數據庫表結構

### bookings 表
| 欄位 | 類型 | 說明 |
| :--- | :--- | :--- |
| id | BIGINT | 主鍵，自動遞增 |
| venue_id | BIGINT | 場地 ID |
| user_id | VARCHAR(20) | 用戶 ID |
| booking_date | DATE | 預約日期 |
| time_slots | INT | 24-bit 位元遮罩 |
| status | TINYINT | 狀態 (0-3) |
| purpose | VARCHAR(255) | 使用用途 |
| p_count | INT | 預估人數 |
| contact_info | JSON | 聯絡資訊 |
| version | INT | 樂觀鎖版本號 |
| created_at | TIMESTAMP | 建立時間 |
| updated_at | TIMESTAMP | 更新時間 |

**索引**
- `idx_venue_date (venue_id, booking_date)` - 查詢衝突優化
- `idx_user_id (user_id)` - 個人申請查詢優化

### booking_equipment 表
| 欄位 | 類型 | 說明 |
| :--- | :--- | :--- |
| id | BIGINT | 主鍵 |
| booking_id | BIGINT | 預約 ID (FK) |
| equipment_id | BIGINT | 設備 ID (FK) |

## 🧪 測試建議

### 單元測試
```java
@Test
public void testCreateBookingWithConflict() {
    // 1. 建立第一筆預約 (8-10 時段)
    Long bookingId1 = bookingService.createBooking(request1);
    
    // 2. 建立衝突的第二筆預約 (9-11 時段) - 應拋出異常
    assertThrows(RuntimeException.class, () -> {
        bookingService.createBooking(request2);
    });
}
```

### 集成測試
```bash
# 使用 curl 或 Postman 測試 API
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -H "Authorization: mock-token-123" \
  -d '{...request body...}'
```

### 並行測試
```java
@Test
public void testConcurrentUpdate() {
    // 模擬多執行緒同時修改同一預約
    // 驗證樂觀鎖是否正確阻擋版本衝突
}
```

## 📚 相關文檔

- **API 設計**：`docs/Design/API_Design/Booking_API_Design.md`
- **Service 設計**：`docs/Design/Service_design/Booking_Service_Design.md`
- **Mapper 設計**：`docs/Design/Mapper_Design/Booking_Mapper_Design.md`
- **資料庫設計**：`docs/Design/DB_Design/function_tables.md`
- **代碼規範**：`.github/skills/enforcing-code-standard/SKILL.md`
- **完成報告**：`docs/BOOKING_MODULE_COMPLETION_REPORT.md`

## 🤝 團隊協作

**代碼審查重點**
- [x] 無 Lambda/Stream API 使用
- [x] 繁體中文註解完整
- [x] 樂觀鎖正確實現
- [x] 事務邊界明確
- [x] 異常處理統一

**部署建議**
1. 執行資料庫遷移 (`venue_tables.sql`)
2. 編譯並打包應用
3. 進行冒煙測試 (Smoke Test)
4. 逐步發佈至測試/預上線環境
5. 進行性能測試

---

**文檔版本**：V1.0  
**最後更新**：2026-04-03  
**維護者**：Venue Reservation Service Team

