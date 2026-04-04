# 場地日曆功能：快速開發指南 (Quick Reference)

**目的**：在開發過程中快速查閱關鍵代碼實現細節  
**更新日期**：2026-04-04

---

## 一、核心代碼片段速查

### 1.1 日期計算工具方法

#### 計算月份範圍
```java
import java.time.YearMonth;
import java.time.LocalDate;

YearMonth ym = YearMonth.of(year, month);
LocalDate startDate = ym.atDay(1);           // 月初
LocalDate endDate = ym.atEndOfMonth();       // 月末
```

#### 計算周範圍（周一開始）
```java
import java.time.DayOfWeek;
import java.time.temporal.ChronoField;

LocalDate weekStart = date.with(ChronoField.DAY_OF_WEEK, 1);  // Monday
LocalDate weekEnd = weekStart.plusDays(6);                      // Sunday

// 或使用 ISO 標準
DayOfWeek dayOfWeek = date.getDayOfWeek();  // 獲取星期幾
if (dayOfWeek != DayOfWeek.MONDAY) {
    throw new IllegalArgumentException("周開始日期必須為周一");
}
```

#### 獲取星期幾名稱
```java
String dayName = date.getDayOfWeek().getDisplayName(
    java.time.format.TextStyle.FULL, 
    java.util.Locale.TRADITIONAL_CHINESE
);
// 結果示例："星期一" 或 "Monday"（取決於 Locale）

// 簡化版本（英文）
String dayOfWeekEn = date.getDayOfWeek().toString();  // "MONDAY"
```

### 1.2 時段轉換工具（已有）

#### Mask → List<Integer>
```java
import tw.edu.ncu.osa.venue_reservation_service.util.BookingUtils;

List<Integer> slots = BookingUtils.parseMaskToList(booking.getTimeSlots());
// 示例：mask=768 → [8, 9]
```

#### List<Integer> → Mask
```java
List<Integer> slots = Arrays.asList(8, 9);
int mask = BookingUtils.convertToMask(slots);  // 768
```

### 1.3 用戶隔離（已有）

#### 獲取當前登入用戶
```java
import tw.edu.ncu.osa.venue_reservation_service.util.UserContext;

String userId = UserContext.getUser().getUserId();
```

### 1.4 JSON 格式化（ObjectMapper）

#### 將物件轉為 JSON 字串
```java
import com.fasterxml.jackson.databind.ObjectMapper;

private final ObjectMapper objectMapper;  // 已注入

String jsonStr = objectMapper.writeValueAsString(object);
```

---

## 二、常見實現模式

### 2.1 Service 方法的標準框架

```java
@Override
@Transactional(readOnly = true)  // 只讀查詢優化
public VenueCalendarMonthVO getVenueCalendarMonth(Long venueId, Integer year, Integer month) {
    // ==========================================
    // 1. 參數驗證 (Guard Clause)
    // ==========================================
    if (venueId == null || venueId <= 0) {
        throw new IllegalArgumentException("場地 ID 不可為空或為負數");
    }
    
    if (year == null || month == null || month < 1 || month > 12) {
        throw new IllegalArgumentException("年份或月份格式不正確");
    }
    
    // 驗證場地是否存在
    Venue venue = venueMapper.selectById(venueId);
    if (venue == null) {
        throw new RuntimeException("場地不存在");
    }
    
    // ==========================================
    // 2. 計算日期範圍
    // ==========================================
    YearMonth ym = YearMonth.of(year, month);
    LocalDate startDate = ym.atDay(1);
    LocalDate endDate = ym.atEndOfMonth();
    
    // ==========================================
    // 3. 數據查詢
    // ==========================================
    String userId = UserContext.getUser().getUserId();
    List<Booking> approvedBookings = bookingMapper.selectApprovedBookingsByDateRange(
        venueId, startDate, endDate);
    List<Booking> userBookings = bookingMapper.selectUserBookingsByDateRange(
        userId, venueId, startDate, endDate);
    
    // ==========================================
    // 4. 數據聚合與轉換
    // ==========================================
    Map<LocalDate, Boolean> approvedMap = new java.util.HashMap<>();
    for (Booking b : approvedBookings) {
        approvedMap.put(b.getBookingDate(), true);
    }
    
    Map<LocalDate, Boolean> userMap = new java.util.HashMap<>();
    for (Booking b : userBookings) {
        userMap.put(b.getBookingDate(), true);
    }
    
    // ==========================================
    // 5. 組裝 VO 並返回
    // ==========================================
    VenueCalendarMonthVO result = new VenueCalendarMonthVO();
    result.setYear(year);
    result.setMonth(month);
    
    List<VenueCalendarMonthVO.DaySimpleSummary> days = new ArrayList<>();
    LocalDate current = startDate;
    while (!current.isAfter(endDate)) {
        VenueCalendarMonthVO.DaySimpleSummary day = new VenueCalendarMonthVO.DaySimpleSummary();
        day.setDate(current.toString());
        day.setHasApprovedBooking(approvedMap.getOrDefault(current, false));
        day.setHasUserBooking(userMap.getOrDefault(current, false));
        days.add(day);
        
        current = current.plusDays(1);
    }
    result.setDays(days);
    
    return result;
}
```

### 2.2 時段列表去重與合併

```java
// 將多個 Booking 的時段合併為一個去重的 List
Set<Integer> slotsSet = new HashSet<>();
for (Booking b : bookings) {
    List<Integer> slots = BookingUtils.parseMaskToList(b.getTimeSlots());
    slotsSet.addAll(slots);
}

// 轉回 List 並排序
List<Integer> mergedSlots = new ArrayList<>(slotsSet);
Collections.sort(mergedSlots);
```

### 2.3 Mapper 參數綁定（XML）

```xml
<!-- 避免 SQL 注入，使用 #{} 進行參數綁定 -->
<select id="selectApprovedBookingsByDateRange" resultType="tw.edu.ncu.osa.venue_reservation_service.model.entity.Booking">
    SELECT id, venue_id, user_id, booking_date, time_slots, status, purpose, p_count, contact_info, version, created_at, updated_at
    FROM bookings
    WHERE venue_id = #{venueId}
      AND booking_date BETWEEN #{startDate} AND #{endDate}
      AND status = 2
    ORDER BY booking_date ASC
</select>
```

---

## 三、常見錯誤與除錯

### 3.1 LocalDate 序列化問題

**問題**：返回 JSON 時 LocalDate 格式錯誤或無法序列化

**解決**：
```java
// 方案 1：使用 @JsonFormat 註解（推薦）
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
public class VenueCalendarDayVO {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
}

// 方案 2：在 VO 中使用 String 而非 LocalDate
private String date;  // "2026-04-06"
```

### 3.2 時段遮罩轉換錯誤

**問題**：轉換結果錯誤或出現意外的時段

**檢查清單**：
- [ ] 是否使用了正確的 BookingUtils 方法？
- [ ] 是否遺漏了時段去重？
- [ ] Booking 物件的 timeSlots 欄位是否正確賦值？

**驗證方法**：
```java
// 手動驗證轉換
int mask = 768;  // 預期 [8, 9]
List<Integer> slots = BookingUtils.parseMaskToList(mask);
System.out.println(slots);  // 應輸出 [8, 9]
```

### 3.3 周邊界計算錯誤

**問題**：跨月周計算錯誤

**檢查清單**：
- [ ] 是否檢查了 weekStartDate 必須為周一？
- [ ] 是否正確計算了 weekEnd = weekStart.plusDays(6)？

**驗證方法**：
```java
LocalDate monday = LocalDate.of(2026, 3, 30);  // 3 月最後一個周一
LocalDate sunday = monday.plusDays(6);  // 4 月 5 日
System.out.println(sunday);  // 應輸出 2026-04-05
```

### 3.4 用戶隔離洩露

**問題**：用戶 A 能看到用戶 B 的預約

**檢查清單**：
- [ ] 是否在查詢用戶預約時帶上了 userId？
- [ ] 是否使用 UserContext.getUser().getUserId() 獲取用戶？
- [ ] 是否有遺漏的 query 路徑導致未篩選用戶？

**驗證方法**：
```java
// 日誌輸出來驗證
log.info("查詢用戶 {} 在場地 {} 的預約", userId, venueId);
List<Booking> bookings = bookingMapper.selectUserBookingsByDateRange(userId, venueId, ...);
log.info("查詢結果數量: {}", bookings.size());

// 確認每條結果的 user_id 都是當前用戶
for (Booking b : bookings) {
    assert b.getUserId().equals(userId);
}
```

---

## 四、測試數據準備

### 4.1 SQL 插入測試數據

```sql
-- 準備測試場地（假設已存在場地 ID=1）

-- 插入已通過預約 (status=2)
-- 2026-04-06 08:00-10:00 (slots [8, 9] → mask 768)
INSERT INTO bookings 
(venue_id, user_id, booking_date, time_slots, status, purpose, p_count, contact_info, version)
VALUES (1, 'admin_user', '2026-04-06', 768, 2, '已通過測試預約 1', 10, '{"name":"Admin","email":"admin@ncu.edu.tw","phone":"0912345678"}', 1);

-- 2026-04-07 14:00-16:00 (slots [14, 15] → mask 49152)
INSERT INTO bookings 
(venue_id, user_id, booking_date, time_slots, status, purpose, p_count, contact_info, version)
VALUES (1, 'other_user', '2026-04-07', 49152, 2, '已通過測試預約 2', 5, '{"name":"Other","email":"other@ncu.edu.tw","phone":"0987654321"}', 1);

-- 插入當前用戶預約（假設當前用戶 ID=current_user）
-- 2026-04-08 15:00-17:00 (slots [15, 16] → mask 98304)，審核中 (status=1)
INSERT INTO bookings 
(venue_id, user_id, booking_date, time_slots, status, purpose, p_count, contact_info, version)
VALUES (1, 'current_user', '2026-04-08', 98304, 1, '我的測試預約', 8, '{"name":"Current","email":"current@ncu.edu.tw","phone":"0912121212"}', 1);

-- 2026-04-09 09:00-11:00 (slots [9, 10] → mask 1536)，已通過 (status=2)
INSERT INTO bookings 
(venue_id, user_id, booking_date, time_slots, status, purpose, p_count, contact_info, version)
VALUES (1, 'current_user', '2026-04-09', 1536, 2, '我的已通過預約', 12, '{"name":"Current","email":"current@ncu.edu.tw","phone":"0912121212"}', 1);
```

### 4.2 時段遮罩計算表

| 時段 | 起點-終點 | 二進位 | 十進位遮罩 |
|:---|:---|:---|:---|
| [8] | 08:00-09:00 | 0000...0100000000 | 256 |
| [8, 9] | 08:00-10:00 | 0000...0110000000 | 768 |
| [14, 15] | 14:00-16:00 | 1100000000000000 | 49152 |
| [15, 16] | 15:00-17:00 | 11000000000000000 | 98304 |
| [9, 10] | 09:00-11:00 | 0000...0110000000 | 1536 |

---

## 五、API 快速測試

### 5.1 使用 cURL 測試

```bash
# 月曆視圖
curl "http://localhost:8080/api/bookings/calendar/month?venueId=1&year=2026&month=4"

# 周曆視圖（2026-04-06 為周一）
curl "http://localhost:8080/api/bookings/calendar/week?venueId=1&date=2026-04-06"

# 日曆視圖
curl "http://localhost:8080/api/bookings/calendar/day?venueId=1&date=2026-04-06"
```

### 5.2 使用 Postman 測試

1. **新建 Request**
   - Method: GET
   - URL: `http://localhost:8080/api/bookings/calendar/month?venueId=1&year=2026&month=4`

2. **頭部設置**
   - Authorization: Bearer {token}（如果啟用了認證）

3. **發送並驗證回應**
   - 狀態碼應為 200
   - Body 應包含 `code: "200"`, `message: "success"`, `data: {...}`

---

## 六、常用 Maven 命令

```bash
# 清理並編譯
mvn clean compile

# 運行全部測試
mvn test

# 運行特定測試類
mvn test -Dtest=BookingServiceImplTest

# 運行特定測試方法
mvn test -Dtest=BookingServiceImplTest#testGetVenueCalendarMonth

# 編譯並跳過測試
mvn clean package -DskipTests

# 查看依賴樹
mvn dependency:tree
```

---

## 七、快速代碼補全片段

### 7.1 完整的 VO 類模板

```java
package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 場地日曆 XXX 視圖 VO (Value Object)
 * 用於 API 回傳場地日曆資料
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VenueCalendarXxxVO {
    
    // ==========================================
    // 基礎資訊
    // ==========================================
    
    /**
     * 場地 ID
     */
    private Long venueId;
    
    /**
     * 場地名稱
     */
    private String venueName;
    
    // ==========================================
    // 查詢範圍
    // ==========================================
    
    /**
     * 開始日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    /**
     * 結束日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    
    // ==========================================
    // 占用情況
    // ==========================================
    
    /**
     * 已通過預約的時段 [0-23]
     */
    private List<Integer> approvedSlots;
    
    /**
     * 當前用戶預約的時段 [0-23]
     */
    private List<Integer> userSlots;
    
    // ==========================================
    // 內部類：日期詳情
    // ==========================================
    
    /**
     * 日期級詳情
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DayDetail {
        // 字段定義
    }
}
```

### 7.2 完整的 Mapper 方法模板

```java
/**
 * 按日期範圍查詢 XXX 預約
 * 
 * @param venueId 場地 ID
 * @param startDate 開始日期
 * @param endDate 結束日期
 * @return 預約實體列表，按日期升序排列
 */
List<Booking> selectXxxBookingsByDateRange(
    @Param("venueId") Long venueId,
    @Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate
);
```

---

## 八、常見問題速查表

| 問題 | 原因 | 解決方案 |
|:---|:---|:---|
| LocalDate 無法序列化為 JSON | 缺少 @JsonFormat 註解 | 加上 `@JsonFormat(pattern = "yyyy-MM-dd")` |
| 獲取用戶 ID 為 null | UserContext 未初始化 | 確認 MockAuthInterceptor 已正確配置 |
| 查詢結果為空 | 測試數據未插入或日期不符 | 檢查 SQL 插入語句與查詢日期範圍 |
| 周邊界計算錯誤 | 未驗證 weekStartDate 為周一 | 加上驗證：`if (date.getDayOfWeek() != DayOfWeek.MONDAY)` |
| 時段列表重複 | 未進行去重 | 使用 `Set<Integer>` 去重，再轉 List |
| 用戶隔離洩露 | 忘記帶 userId 參數 | 檢查所有用戶相關查詢都帶上 userId |

---

**最後更新**：2026-04-04  
**維護者**：開發團隊  
**反饋渠道**：在團隊 Wiki 留言或提交 PR

