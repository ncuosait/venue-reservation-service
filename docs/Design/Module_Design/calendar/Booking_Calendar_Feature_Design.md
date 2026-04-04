# 預約系統場地日曆功能設計文件 (V1.0)

**日期：** 2026-04-04
**狀態：** 詳細設計階段
**功能：** 場地預約日曆視圖（月/周/日）

---

## 一、 功能概述 (Feature Overview)

### 1.1 功能定義
用戶在選擇單位與場地後，進入該場地的預約日曆主頁。系統提供**三層級別日曆視圖**（月曆、周曆、日曆），用戶可在三種視圖間自由切換。

每種視圖均顯示：
- **已占用時段**：該場地已通過審核（status=2）的預約時段
- **用戶預約時段**：當前登入用戶在該場地的所有預約（無論審核狀態）

### 1.2 業務價值
- **用戶體驗優化**：直觀的日曆視圖幫助用戶快速識別場地可用時段
- **資訊可視化**：區分已占用與自己預約，便於管理多筆申請
- **流程簡化**：進場地即見月曆，無需額外導航

---

## 二、 資料庫可行性分析

### 2.1 現有表結構滿足度評估

#### bookings 表結構
```
CREATE TABLE bookings (
    id BIGINT,
    venue_id BIGINT,           ← 日曆查詢過濾
    user_id VARCHAR(20),       ← 用戶隔離
    booking_date DATE,         ← 日期範圍查詢
    time_slots INT UNSIGNED,   ← 24-bit 時段遮罩
    status TINYINT,            ← 狀態篩選 (2 = 已通過)
    ...
    INDEX idx_venue_date (venue_id, booking_date)  ← 複合索引支持高效範圍查詢
);
```

**評估結論：✓ 完全滿足**

### 2.2 查詢性能分析

#### 場景 1：查詢某場地某月已通過預約
```sql
SELECT * FROM bookings
WHERE venue_id = #{venueId}
  AND booking_date BETWEEN #{startDate} AND #{endDate}
  AND status = 2;
```
**性能：** 利用 `idx_venue_date` 索引，複雜度 O(log N)

#### 場景 2：查詢用戶在某場地某月的全部預約
```sql
SELECT * FROM bookings
WHERE venue_id = #{venueId}
  AND user_id = #{userId}
  AND booking_date BETWEEN #{startDate} AND #{endDate};
```
**性能：** 先用 `idx_venue_date` 快速定位記錄集，再在應用層篩選用戶 ID，複雜度 O(log N + M)，其中 M 為該期間該場地的預約數量（通常很小）

#### 場景 3：時段衝突判定（已在現有系統中驗證）
```sql
SELECT COUNT(*) FROM bookings
WHERE venue_id = #{venueId}
  AND booking_date = #{date}
  AND status = 2
  AND (time_slots & #{mask}) != 0;
```
**性能：** 位元運算直接在 SQL 層執行，無需應用層轉換

### 2.3 無需數據表修改
✓ 現有結構完全支持所有三種視圖的查詢需求  
✓ 無需新增欄位或索引

---

## 三、 功能架構設計

### 3.1 分層次設計

```
前端層 (Frontend)
    ├─ 月曆視圖 (Month Calendar)
    ├─ 周曆視圖 (Week Calendar)
    └─ 日曆視圖 (Day Calendar)
            ↓
REST API 層 (Controller)
    ├─ GET /api/bookings/calendar/month?venueId={id}&year={y}&month={m}
    ├─ GET /api/bookings/calendar/week?venueId={id}&date={weekStartDate}
    └─ GET /api/bookings/calendar/day?venueId={id}&date={date}
            ↓
業務服務層 (Service)
    ├─ getVenueCalendarMonth(venueId, year, month)
    ├─ getVenueCalendarWeek(venueId, weekStartDate)
    └─ getVenueCalendarDay(venueId, date)
            ↓
數據持久層 (Mapper)
    ├─ selectApprovedBookingsByDateRange(venueId, startDate, endDate, status)
    └─ selectUserBookingsByDateRange(userId, venueId, startDate, endDate)
            ↓
數據庫層 (Database)
    └─ bookings 表 (利用 idx_venue_date 索引)
```

### 3.2 數據流向

#### 月曆視圖（Month Calendar View）
```
用戶訪問 /calendar/month?venueId=1&year=2026&month=4

↓ Service 層計算日期範圍
startDate = 2026-04-01, endDate = 2026-04-30

↓ Mapper 查詢
Query 1: 已通過預約 (status=2, 日期範圍)
Query 2: 用戶預約 (user_id=當前用戶, 日期範圍)

↓ Service 層聚合 (無需詳細時段)
返回: {
    year: 2026,
    month: 4,
    days: [
        { date: "2026-04-01", hasApprovedBooking: true, hasUserBooking: false },
        { date: "2026-04-02", hasApprovedBooking: false, hasUserBooking: true },
        ...
    ]
}
```

**優化策略**：月視圖只返回有無預約的標記，不詳列時段，減少數據傳輸

#### 周曆視圖（Week Calendar View）
```
用戶訪問 /calendar/week?venueId=1&date=2026-04-06 (週一)

↓ Service 層計算日期範圍
startDate = 2026-04-06, endDate = 2026-04-12

↓ Mapper 查詢 (同上)

↓ Service 層聚合 (詳細時段)
返回: {
    weekStart: "2026-04-06",
    weekEnd: "2026-04-12",
    days: [
        {
            date: "2026-04-06",
            dayOfWeek: "Monday",
            approvedSlots: [9, 10, 11],      // 已通過預約的時段
            userSlots: [14, 15, 16]          // 用戶預約的時段
        },
        ...
    ]
}
```

#### 日曆視圖（Day Calendar View）
```
用戶訪問 /calendar/day?venueId=1&date=2026-04-06

↓ Mapper 查詢
Query 1: 該日已通過預約
Query 2: 用戶在該日的預約

↓ Service 層聚合
返回: {
    date: "2026-04-06",
    venueId: 1,
    venueName: "會議室 A",
    approvedSlots: [9, 10, 11, 14],
    userSlots: [15, 16, 17],
    userBookingDetails: [                  // 用戶預約詳情
        {
            bookingId: 101,
            slots: [15, 16, 17],
            status: 1,                       // 審核中
            purpose: "課程講座"
        }
    ]
}
```

---

## 四、 技術實現方案

### 4.1 新增 VO 類設計

#### VenueCalendarMonthVO.java
```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VenueCalendarMonthVO {
    // 年月資訊
    private Integer year;
    private Integer month;
    
    // 該月每日摘要
    private List<DaySimpleSummary> days;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DaySimpleSummary {
        private String date;           // ISO 8601 格式: "2026-04-01"
        private Boolean hasApprovedBooking;  // 是否有已通過預約
        private Boolean hasUserBooking;      // 是否有用戶預約
    }
}
```

#### VenueCalendarWeekVO.java
```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VenueCalendarWeekVO {
    // 周資訊
    private String weekStart;  // "2026-04-06"
    private String weekEnd;    // "2026-04-12"
    
    // 周內每日詳細資訊
    private List<DayDetailSummary> days;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DayDetailSummary {
        private String date;
        private String dayOfWeek;           // "Monday", "Tuesday", ...
        private List<Integer> approvedSlots;  // 已通過的時段 [0-23]
        private List<Integer> userSlots;      // 用戶預約的時段 [0-23]
    }
}
```

#### VenueCalendarDayVO.java
```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VenueCalendarDayVO {
    // 場地與日期
    private Long venueId;
    private String venueName;
    private String date;  // "2026-04-06"
    private String dayOfWeek;  // "Monday"
    
    // 時段占用情況
    private List<Integer> approvedSlots;  // 已通過預約時段 [0-23]
    private List<Integer> userSlots;      // 用戶預約時段 [0-23]
    
    // 用戶預約詳情（點擊時段時可展示）
    private List<UserBookingDetail> userBookingDetails;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserBookingDetail {
        private Long bookingId;
        private List<Integer> slots;
        private Integer status;  // 0: 撤回, 1: 審核中, 2: 通過, 3: 拒絕
        private String purpose;
        private LocalDateTime createdAt;
    }
}
```

### 4.2 Mapper 層新增方法

#### BookingMapper.java (Interface)
```java
/**
 * 按日期範圍查詢已通過的預約
 * @param venueId 場地 ID
 * @param startDate 開始日期
 * @param endDate 結束日期
 * @return 預約實體列表
 */
List<Booking> selectApprovedBookingsByDateRange(
    @Param("venueId") Long venueId,
    @Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate
);

/**
 * 按日期範圍查詢用戶在特定場地的所有預約
 * @param userId 用戶 ID
 * @param venueId 場地 ID
 * @param startDate 開始日期
 * @param endDate 結束日期
 * @return 預約實體列表
 */
List<Booking> selectUserBookingsByDateRange(
    @Param("userId") String userId,
    @Param("venueId") Long venueId,
    @Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate
);
```

#### BookingMapper.xml (SQL)
```xml
<!-- 已通過預約查詢 -->
<select id="selectApprovedBookingsByDateRange" resultType="tw.edu.ncu.osa.venue_reservation_service.model.entity.Booking">
    SELECT id, venue_id, user_id, booking_date, time_slots, status, purpose, p_count, contact_info, version, created_at, updated_at
    FROM bookings
    WHERE venue_id = #{venueId}
      AND booking_date BETWEEN #{startDate} AND #{endDate}
      AND status = 2
    ORDER BY booking_date ASC
</select>

<!-- 用戶預約查詢 -->
<select id="selectUserBookingsByDateRange" resultType="tw.edu.ncu.osa.venue_reservation_service.model.entity.Booking">
    SELECT id, venue_id, user_id, booking_date, time_slots, status, purpose, p_count, contact_info, version, created_at, updated_at
    FROM bookings
    WHERE venue_id = #{venueId}
      AND user_id = #{userId}
      AND booking_date BETWEEN #{startDate} AND #{endDate}
    ORDER BY booking_date ASC
</select>
```

### 4.3 Service 層新增方法

#### BookingService.java (Interface)
```java
/**
 * 獲取場地月曆視圖
 * @param venueId 場地 ID
 * @param year 年份
 * @param month 月份 (1-12)
 * @return 月曆視圖資料
 */
VenueCalendarMonthVO getVenueCalendarMonth(Long venueId, Integer year, Integer month);

/**
 * 獲取場地周曆視圖
 * @param venueId 場地 ID
 * @param weekStartDate 周開始日期 (周一)
 * @return 周曆視圖資料
 */
VenueCalendarWeekVO getVenueCalendarWeek(Long venueId, LocalDate weekStartDate);

/**
 * 獲取場地日曆視圖
 * @param venueId 場地 ID
 * @param date 查詢日期
 * @return 日曆視圖資料
 */
VenueCalendarDayVO getVenueCalendarDay(Long venueId, LocalDate date);
```

### 4.4 Controller 層新增端點

#### BookingController.java (REST API)
```java
/**
 * 獲取場地月曆視圖
 * @param venueId 場地 ID (query param)
 * @param year 年份 (query param)
 * @param month 月份 (query param)
 * @return 月曆資料
 */
@GetMapping("/calendar/month")
public Result<VenueCalendarMonthVO> getCalendarMonth(
    @RequestParam Long venueId,
    @RequestParam Integer year,
    @RequestParam Integer month) {
    VenueCalendarMonthVO result = bookingService.getVenueCalendarMonth(venueId, year, month);
    return Result.success(result);
}

/**
 * 獲取場地周曆視圖
 * @param venueId 場地 ID (query param)
 * @param date 周開始日期 (query param, ISO 8601 格式)
 * @return 周曆資料
 */
@GetMapping("/calendar/week")
public Result<VenueCalendarWeekVO> getCalendarWeek(
    @RequestParam Long venueId,
    @RequestParam LocalDate date) {
    VenueCalendarWeekVO result = bookingService.getVenueCalendarWeek(venueId, date);
    return Result.success(result);
}

/**
 * 獲取場地日曆視圖
 * @param venueId 場地 ID (query param)
 * @param date 查詢日期 (query param, ISO 8601 格式)
 * @return 日曆資料
 */
@GetMapping("/calendar/day")
public Result<VenueCalendarDayVO> getCalendarDay(
    @RequestParam Long venueId,
    @RequestParam LocalDate date) {
    VenueCalendarDayVO result = bookingService.getVenueCalendarDay(venueId, date);
    return Result.success(result);
}
```

---

## 五、 實現細節與最佳實踐

### 5.1 日期範圍計算
```java
// 月份範圍
YearMonth ym = YearMonth.of(year, month);
LocalDate startDate = ym.atDay(1);
LocalDate endDate = ym.atEndOfMonth();

// 周範圍（周一開始）
LocalDate weekStart = date.with(ChronoField.DAY_OF_WEEK, 1);  // Monday
LocalDate weekEnd = weekStart.plusDays(6);

// 日期範圍
LocalDate startDate = date;
LocalDate endDate = date;
```

### 5.2 時段轉換
```java
// 將 time_slots (int) 轉換為 List<Integer>
List<Integer> slots = BookingUtils.parseMaskToList(booking.getTimeSlots());

// 示例：mask = 768 (二進位: 0011 0000 0000) → [8, 9]
```

### 5.3 用戶隔離
```java
// 在 Service 層獲取當前用戶
String userId = UserContext.getUser().getUserId();

// 查詢用戶預約時必須帶上 userId
List<Booking> userBookings = bookingMapper.selectUserBookingsByDateRange(userId, venueId, startDate, endDate);
```

### 5.4 性能優化建議

#### 查詢優化
- ✓ 利用 `idx_venue_date` 複合索引
- ✓ 保持兩次獨立查詢（已通過 + 用戶預約），避免複雜 JOIN
- ✓ 月視圖無需詳細時段，可在 SQL 層只 SELECT 必要欄位

#### 緩存策略（可選）
```java
@Cacheable(
    value = "venueApprovedBookings",
    key = "'approved_' + #venueId + '_' + #startDate + '_' + #endDate",
    cacheManager = "cacheManager"
)
public List<Booking> getApprovedBookings(Long venueId, LocalDate startDate, LocalDate endDate) {
    return bookingMapper.selectApprovedBookingsByDateRange(venueId, startDate, endDate);
}
```

**說明**：已通過預約較為穩定，可使用 Redis 快取；用戶預約則不快取（即時性要求）。

### 5.5 異常處理
```java
if (venueId == null || venueId <= 0) {
    throw new IllegalArgumentException("場地 ID 不可為空或為負數");
}

if (year < 1900 || year > 2100 || month < 1 || month > 12) {
    throw new IllegalArgumentException("年份或月份格式不正確");
}

// 檢查場地是否存在
Venue venue = venueMapper.selectById(venueId);
if (venue == null) {
    throw new RuntimeException("場地不存在");
}
```

---

## 六、 前端集成指南

### 6.1 使用流程
```
1. 用戶選擇單位 → GET /api/public/units
2. 用戶選擇場地 → GET /api/public/venues?unitId={unitId}
3. 進入場地主頁 → 自動調用 GET /api/bookings/calendar/month?venueId={id}&year={currentYear}&month={currentMonth}
4. 用戶可切換視圖：
   - 點擊月曆某日 → GET /api/bookings/calendar/day?venueId={id}&date={date}
   - 前後切換週 → GET /api/bookings/calendar/week?venueId={id}&date={weekStartDate}
   - 前後切換月 → GET /api/bookings/calendar/month?venueId={id}&year={year}&month={month}
```

### 6.2 API 回應範例

#### 月曆視圖回應
```json
{
    "code": "200",
    "message": "success",
    "data": {
        "year": 2026,
        "month": 4,
        "days": [
            {
                "date": "2026-04-01",
                "hasApprovedBooking": true,
                "hasUserBooking": false
            },
            {
                "date": "2026-04-02",
                "hasApprovedBooking": false,
                "hasUserBooking": true
            }
        ]
    }
}
```

#### 周曆視圖回應
```json
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
            }
        ]
    }
}
```

#### 日曆視圖回應
```json
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

## 七、 開發清單

- [ ] 新建 VO 類：`VenueCalendarMonthVO.java`
- [ ] 新建 VO 類：`VenueCalendarWeekVO.java`
- [ ] 新建 VO 類：`VenueCalendarDayVO.java`
- [ ] 修改 `BookingMapper.java`，新增兩個查詢方法
- [ ] 修改 `BookingMapper.xml`，新增兩個 SQL 語句
- [ ] 修改 `BookingService.java` 介面，新增三個業務方法
- [ ] 修改 `BookingServiceImpl.java`，實現三個業務方法邏輯
- [ ] 修改 `BookingController.java`，新增三個 REST 端點
- [ ] 編寫單元測試 (使用 JUnit 5 + Mockito)
- [ ] 編寫集成測試 (驗證 API 回應)
- [ ] 調整日期邊界情況測試（跨月、跨年）

---

## 八、 風險與緩解方案

### 8.1 潛在風險

| 風險項 | 可能性 | 影響 | 緩解方案 |
|:---|:---|:---|:---|
| 大量查詢同時到達導致性能下降 | 中 | 高 | 加入 Redis 緩存，對已通過預約做快取策略 |
| 時段遮罩轉換錯誤 | 低 | 中 | 現有工具已驗證，保持使用 |
| 跨時區日期計算錯誤 | 低 | 中 | 統一使用 LocalDate (無時區)，確保一致性 |
| 用戶 ID 隱私洩露 | 中 | 高 | 確保所有用戶查詢都經過 UserContext 隔離 |

### 8.2 測試策略
- 邊界測試：月初月末、年初年末、周邊界
- 性能測試：大數據量下的查詢響應時間
- 安全測試：檢驗用戶隔離邏輯是否完善

---

## 九、 相關文件參考

- [DB_Design/function_tables.md](../../DB_Design/function_tables.md) - 數據表設計
- [Booking_Module_Design.md](../Booking_Module_Design.md) - 預約核心引擎設計
- [Booking_Service_Design.md](../../Service_design/Booking_Service_Design.md) - 業務層設計
- [Booking_Mapper_Design.md](../../Mapper_Design/Booking_Mapper_Design.md) - 持久層設計

---

**文件版本**：V1.0  
**最後更新**：2026-04-04

