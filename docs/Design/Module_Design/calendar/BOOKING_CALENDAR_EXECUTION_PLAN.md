# 場地日曆功能開發：執行計劃最終總結

**項目代號**：Booking_Calendar_Feature_V1.0  
**業務需求**：支持用戶在場地選擇後查看月/周/日三層次日曆，顯示已占用時段和用戶預約  
**預計工期**：1-2 個工作天 (8-12 小時)  
**優先級**：高  
**難度等級**：中等

---

## 執行摘要

### 核心成果物
✅ **設計文檔**：`docs/Design/Module_Design/Booking_Calendar_Feature_Design.md`  
✅ **開發清單**：`docs/dev-process/Booking_Module/BOOKING_CALENDAR_FEATURE_CHECKLIST.md`  
✅ **快速指南**：`docs/dev-process/Booking_Module/BOOKING_CALENDAR_FEATURE_QUICK_GUIDE.md`

### 技術可行性評估結果
**評估狀態**：✅ **完全可行，無需修改數據表**

**原因**：
1. 現有複合索引 `idx_venue_date(venue_id, booking_date)` 完美支持日期範圍查詢
2. 24-bit 時段遮罩已成熟驗證，轉換工具齊全
3. 現有 Booking 表結構無需任何修改
4. 用戶隔離機制（UserContext）已完善

---

## 開發架構概覽

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端層 (Frontend)                         │
│   月曆視圖         周曆視圖          日曆視圖                     │
│   (Monthly)        (Weekly)          (Daily)                      │
└──────────────┬──────────────────────┬──────────────────────────┘
               │                      │
               ▼                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                    REST API 層 (Controller)                      │
│  GET /bookings/calendar/month                                    │
│  GET /bookings/calendar/week                                     │
│  GET /bookings/calendar/day                                      │
└──────────────┬──────────────────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────────────────┐
│             業務服務層 (Service - 數據聚合與轉換)                 │
│  getVenueCalendarMonth()  → VenueCalendarMonthVO                │
│  getVenueCalendarWeek()   → VenueCalendarWeekVO                 │
│  getVenueCalendarDay()    → VenueCalendarDayVO                  │
└──────────────┬──────────────────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────────────────┐
│             持久層 (Mapper - 數據庫查詢)                         │
│  selectApprovedBookingsByDateRange()                             │
│  selectUserBookingsByDateRange()                                 │
└──────────────┬──────────────────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────────────────┐
│              數據庫層 (Database - bookings 表)                    │
│  [已通過預約 status=2] + [用戶預約 任何狀態]                      │
└─────────────────────────────────────────────────────────────────┘
```

---

## 分階段開發計劃

### 第 1-2 天：基礎實現

#### Phase 1：VO 類設計（2-3 小時）
```
新建 3 個 VO 類：
├─ VenueCalendarMonthVO.java        (月視圖：簡化版，只標記有無預約)
├─ VenueCalendarWeekVO.java         (周視圖：詳細版，含時段列表)
└─ VenueCalendarDayVO.java          (日視圖：最詳細版，含預約清單)
```

**預期成果**：3 個可編譯的 VO 類，具備完整 Javadoc 與 Lombok 註解

---

#### Phase 2：Mapper 層擴展（1.5-2 小時）
```
修改 BookingMapper.java（接口）：
├─ + selectApprovedBookingsByDateRange(venueId, startDate, endDate)
└─ + selectUserBookingsByDateRange(userId, venueId, startDate, endDate)

修改 BookingMapper.xml（SQL 實現）：
├─ + SQL: 按日期範圍查詢已通過預約 (status=2)
└─ + SQL: 按日期範圍查詢用戶預約
```

**預期成果**：2 個 Mapper 方法編譯通過，SQL 執行無誤

**測試方法**：
```bash
mvn clean compile
# 手動 INSERT 測試數據，驗證 SQL 查詢結果正確
```

---

#### Phase 3：Service 層實現（2-2.5 小時）
```
修改 BookingService.java（接口）：
├─ + getVenueCalendarMonth(venueId, year, month)
├─ + getVenueCalendarWeek(venueId, weekStartDate)
└─ + getVenueCalendarDay(venueId, date)

修改 BookingServiceImpl.java（實現）：
├─ 實現月視圖邏輯：日期計算 → 數據聚合 → VO 組裝
├─ 實現周視圖邏輯：周邊界驗證 → 詳細時段轉換 → VO 組裝
└─ 實現日視圖邏輯：用戶預約詳情提取 → VO 組裝
```

**預期成果**：3 個 Service 方法實現完整，包含參數驗證、數據聚合、異常處理

**關鍵實現細節**：
- 使用 Guard Clause 進行參數驗證
- 利用 Map 進行 O(1) 查詢時間複雜度的數據聚合
- 使用 Set 進行時段去重
- 加上 `@Transactional(readOnly = true)` 優化只讀查詢

---

#### Phase 4：Controller 層實現（1 小時）
```
修改 BookingController.java：
├─ + @GetMapping("/calendar/month") getCalendarMonth()
├─ + @GetMapping("/calendar/week") getCalendarWeek()
└─ + @GetMapping("/calendar/day") getCalendarDay()
```

**預期成果**：3 個 REST 端點可正常調用，回應格式符合設計

---

#### Phase 5：測試與驗證（1.5-2 小時）
```
單元測試：
├─ BookingCalendarServiceTest
│  ├─ testGetVenueCalendarMonth_Normal()
│  ├─ testGetVenueCalendarMonth_EdgeCase()
│  ├─ testGetVenueCalendarWeek_Normal()
│  └─ testGetVenueCalendarDay_UserIsolation()
└─ BookingCalendarControllerTest
   ├─ testMonthAPI_Success()
   └─ testMonthAPI_InvalidParams()

集成測試：
├─ 準備 5+ 筆已通過預約
├─ 準備 3+ 筆用戶預約
└─ 驗證 API 回應數據正確聚合
```

**驗收標準**：
- [ ] 所有方法編譯通過，無警告
- [ ] mvn test 全部通過
- [ ] 三個 API 端點均可正常訪問
- [ ] 時段轉換、日期計算、用戶隔離均驗證無誤

---

## 新增代碼清單

### 文件清單（共 6 個新/修改文件）

| 序號 | 文件路徑 | 類型 | 行數 | 備註 |
|:---|:---|:---|:---|:---|
| 1 | `src/main/java/.../vo/VenueCalendarMonthVO.java` | 新建 | ~50 | VO 類 |
| 2 | `src/main/java/.../vo/VenueCalendarWeekVO.java` | 新建 | ~60 | VO 類 |
| 3 | `src/main/java/.../vo/VenueCalendarDayVO.java` | 新建 | ~80 | VO 類 |
| 4 | `src/main/java/.../mapper/BookingMapper.java` | 修改 | +2 方法 | 接口擴展 |
| 5 | `src/main/resources/mapper/BookingMapper.xml` | 修改 | +2 SQL | XML 擴展 |
| 6 | `src/main/java/.../service/BookingService.java` | 修改 | +3 方法 | 接口擴展 |
| 7 | `src/main/java/.../service/impl/BookingServiceImpl.java` | 修改 | +120 代碼行 | 實現擴展 |
| 8 | `src/main/java/.../controller/BookingController.java` | 修改 | +3 方法 | 端點擴展 |

**總代碼量**：~450 行（含註解與空行）

---

## API 契約速覽

### API 1：月曆視圖
```
GET /api/bookings/calendar/month?venueId={id}&year={y}&month={m}

Response 200 OK:
{
    "code": "200",
    "message": "success",
    "data": {
        "year": 2026,
        "month": 4,
        "days": [
            {"date": "2026-04-01", "hasApprovedBooking": true, "hasUserBooking": false},
            {"date": "2026-04-02", "hasApprovedBooking": false, "hasUserBooking": true},
            ...
        ]
    }
}
```

### API 2：周曆視圖
```
GET /api/bookings/calendar/week?venueId={id}&date=2026-04-06

Response 200 OK:
{
    "code": "200",
    "message": "success",
    "data": {
        "weekStart": "2026-04-06",
        "weekEnd": "2026-04-12",
        "days": [
            {
                "date": "2026-04-06",
                "dayOfWeek": "Monday",
                "approvedSlots": [9, 10, 11],
                "userSlots": [14, 15, 16]
            },
            ...
        ]
    }
}
```

### API 3：日曆視圖
```
GET /api/bookings/calendar/day?venueId={id}&date=2026-04-06

Response 200 OK:
{
    "code": "200",
    "message": "success",
    "data": {
        "venueId": 1,
        "venueName": "會議室 A",
        "date": "2026-04-06",
        "dayOfWeek": "Monday",
        "approvedSlots": [9, 10, 11, 14],
        "userSlots": [15, 16, 17],
        "userBookingDetails": [
            {
                "bookingId": 101,
                "slots": [15, 16, 17],
                "status": 1,
                "purpose": "課程講座",
                "createdAt": "2026-04-01T10:00:00"
            }
        ]
    }
}
```

---

## 性能指標

### 查詢性能目標

| 操作 | 數據量 | 預期響應時間 | 說明 |
|:---|:---|:---|:---|
| 月曆查詢 | 30 天，每天最多 100 筆預約 | < 100ms | 利用 idx_venue_date 索引 |
| 周曆查詢 | 7 天，每天最多 100 筆預約 | < 200ms | 包含詳細時段轉換 |
| 日曆查詢 | 單日，最多 50 筆預約 | < 100ms | 最輕量級查詢 |

### 數據庫優化策略

✅ **利用現有索引**：`idx_venue_date(venue_id, booking_date)` 優化日期範圍查詢

✅ **兩次獨立查詢**：比複雜 JOIN 更清晰，性能等效

✅ **只讀事務**：Service 層方法標註 `@Transactional(readOnly = true)` 優化查詢

✅ **緩存備選方案**（可選）：已通過預約可使用 Redis 快取，減輕數據庫壓力

---

## 風險管理

### 識別的風險及緩解方案

| 風險 | 可能性 | 嚴重性 | 緩解方案 |
|:---|:---|:---|:---|
| 跨月/跨年日期計算錯誤 | 低 | 高 | 編寫邊界情況測試；使用標準 LocalDate API |
| 時段遮罩轉換錯誤 | 低 | 中 | 保持使用現有驗證過的 BookingUtils；補充單元測試 |
| 用戶隔離洩露 | 中 | 極高 | 代碼審查時重點檢查；編寫隔離測試用例 |
| 大量查詢導致性能下降 | 中 | 中 | 準備 Redis 快取方案；監控查詢執行計畫 |
| LocalDate 序列化問題 | 低 | 低 | 使用 @JsonFormat 註解；優先使用 String 類型存儲日期 |

---

## 完成後的驗收項目

### 功能驗收
- [ ] 用戶進入場地後，預設顯示當月日曆視圖
- [ ] 月曆視圖可正確標記有無預約的日期
- [ ] 可從月曆切換至周曆和日曆
- [ ] 周曆和日曆顯示詳細時段信息
- [ ] 已通過預約與用戶預約清晰區分
- [ ] 支持前後切換月份/周次

### 代碼品質驗收
- [ ] 全部代碼通過 `mvn clean compile` 編譯
- [ ] 無 Checkstyle 違規（命名、註解規範）
- [ ] 所有方法有繁體中文 Javadoc
- [ ] 無使用 Lambda/Stream 表達式
- [ ] 異常處理完善，無空 catch 塊

### 安全性驗收
- [ ] 用戶只能查看自己的預約（通過 UserContext 隔離）
- [ ] 參數驗證完整（非空、範圍、格式）
- [ ] SQL 無注入風險（使用 #{} 參數綁定）

### 性能驗收
- [ ] 月曆查詢 < 100ms
- [ ] 周曆查詢 < 200ms
- [ ] 日曆查詢 < 100ms

---

## 關鍵文檔導航

| 文檔 | 用途 | 路徑 |
|:---|:---|:---|
| **功能設計文件** | 詳細設計與可行性分析 | `docs/Design/Module_Design/Booking_Calendar_Feature_Design.md` |
| **開發清單** | 逐步開發指南與驗收標準 | `docs/dev-process/Booking_Module/BOOKING_CALENDAR_FEATURE_CHECKLIST.md` |
| **快速指南** | 開發時速查代碼片段與常見錯誤 | `docs/dev-process/Booking_Module/BOOKING_CALENDAR_FEATURE_QUICK_GUIDE.md` |
| **DB 設計** | 數據表結構參考 | `docs/Design/DB_Design/function_tables.md` |
| **預約模組設計** | 現有預約系統的核心設計 | `docs/Design/Module_Design/Booking_Module_Design.md` |

---

## 後續擴展建議

### 短期（Phase 2）
1. **緩存優化**：為已通過預約加入 Redis 快取層，減輕數據庫查詢
2. **時間選擇優化**：在日視圖上支持快速時段選擇與直接預約
3. **移動端適配**：針對小屏幕優化日曆展示（可能需要 UI 調整）

### 中期（Phase 3）
1. **批量操作**：支持用戶同時預約多個日期的同一時段
2. **預約提醒**：在日曆視圖上集成提醒功能
3. **導出功能**：支持將日曆數據導出為 ICS/Excel

### 長期（Phase 4）
1. **審核審計日誌**：在日曆視圖顯示審核歷史
2. **衝突提醒**：在用戶嘗試預約時實時提示時段衝突
3. **容量管理**：顯示場地容納人數與當前預約人數對比

---

## 附錄：技術棧速查

| 層級 | 技術棧 | 版本 |
|:---|:---|:---|
| 前端 | Spring Boot Web (未提及，由前端團隊負責) | - |
| API | Spring Web REST | 6.0+ |
| 業務層 | Spring Service / @Transactional | - |
| 持久層 | MyBatis | 3.5.10+ |
| 數據庫 | MySQL | 5.7+/8.0+ |
| 工具類 | Lombok, Jackson | 1.18+, 2.15+ |
| 測試 | JUnit 5, Mockito | 5.8+, 5.2+ |
| 構建 | Maven | 3.8.1+ |

---

## 最後

本規劃已通過以下驗證：
✅ 數據庫架構可行性評估完成  
✅ API 契約設計完整  
✅ 開發流程清晰有序  
✅ 風險識別與緩解方案完善  
✅ 代碼品質標準明確  

**建議立即啟動開發，預計 1-2 個工作天內完成交付。**

---

**文檔編寫日期**：2026-04-04  
**審核狀態**：待團隊確認  
**最後修改**：2026-04-04

