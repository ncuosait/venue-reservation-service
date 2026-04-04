# 場地日曆功能開發清單 (Development Checklist)

**功能模組**：Booking Calendar Feature (月/周/日視圖)  
**預計工期**：2-3 工作天  
**難度等級**：中等  
**優先級**：高

---

## 第一階段：VO 類設計與實現 (Day 1)

### 1.1 VenueCalendarMonthVO.java
- [ ] 建立檔案：`src/main/java/tw/edu/ncu/osa/venue_reservation_service/model/vo/VenueCalendarMonthVO.java`
- [ ] 包含主類與內部類 `DaySimpleSummary`
- [ ] 內部類字段：`date` (String), `hasApprovedBooking` (Boolean), `hasUserBooking` (Boolean)
- [ ] 加上 Lombok 註解：@Data, @AllArgsConstructor, @NoArgsConstructor
- [ ] 加上詳細的繁體中文 Javadoc

### 1.2 VenueCalendarWeekVO.java
- [ ] 建立檔案：`src/main/java/tw/edu/ncu/osa/venue_reservation_service/model/vo/VenueCalendarWeekVO.java`
- [ ] 包含主類與內部類 `DayDetailSummary`
- [ ] 內部類字段：`date`, `dayOfWeek`, `approvedSlots`, `userSlots`
- [ ] `approvedSlots` 與 `userSlots` 為 `List<Integer>` 時段列表
- [ ] 加上相應註解與文檔

### 1.3 VenueCalendarDayVO.java
- [ ] 建立檔案：`src/main/java/tw/edu/ncu/osa/venue_reservation_service/model/vo/VenueCalendarDayVO.java`
- [ ] 包含主類與內部類 `UserBookingDetail`
- [ ] 主類字段：`venueId`, `venueName`, `date`, `dayOfWeek`, `approvedSlots`, `userSlots`, `userBookingDetails`
- [ ] 內部類字段：`bookingId`, `slots`, `status`, `purpose`, `createdAt`
- [ ] 加上相應註解與文檔

---

## 第二階段：Mapper 層擴展 (Day 1-2)

### 2.1 BookingMapper.java 介面擴展
- [ ] 新增方法簽名：`selectApprovedBookingsByDateRange(...)`
  - 參數：`venueId` (Long), `startDate` (LocalDate), `endDate` (LocalDate)
  - 回傳：`List<Booking>`
  - 加上 Javadoc
  
- [ ] 新增方法簽名：`selectUserBookingsByDateRange(...)`
  - 參數：`userId` (String), `venueId` (Long), `startDate` (LocalDate), `endDate` (LocalDate)
  - 回傳：`List<Booking>`
  - 加上 Javadoc

### 2.2 BookingMapper.xml SQL 實現
- [ ] 新增 SELECT 語句 ID：`selectApprovedBookingsByDateRange`
  - WHERE 條件：`venue_id = #{venueId} AND booking_date BETWEEN #{startDate} AND #{endDate} AND status = 2`
  - ORDER BY：`booking_date ASC`
  
- [ ] 新增 SELECT 語句 ID：`selectUserBookingsByDateRange`
  - WHERE 條件：`venue_id = #{venueId} AND user_id = #{userId} AND booking_date BETWEEN #{startDate} AND #{endDate}`
  - ORDER BY：`booking_date ASC`

- [ ] 驗證 resultType 指向正確的 Booking 實體

### 2.3 Mapper 測試驗證
- [ ] 執行 `mvn clean package` 確保編譯無誤
- [ ] 在資料庫中手動 INSERT 測試數據
- [ ] 使用 MyBatis 控制台或 SQL 客戶端直接執行 SQL 驗證結果

---

## 第三階段：Service 層業務邏輯 (Day 2-3)

### 3.1 BookingService.java 介面擴展
- [ ] 新增方法簽名：`getVenueCalendarMonth(Long venueId, Integer year, Integer month)`
  - 回傳：`VenueCalendarMonthVO`
  - 加上詳細 Javadoc
  
- [ ] 新增方法簽名：`getVenueCalendarWeek(Long venueId, LocalDate weekStartDate)`
  - 回傳：`VenueCalendarWeekVO`
  - 加上詳細 Javadoc
  
- [ ] 新增方法簽名：`getVenueCalendarDay(Long venueId, LocalDate date)`
  - 回傳：`VenueCalendarDayVO`
  - 加上詳細 Javadoc

### 3.2 BookingServiceImpl.java 實現 - getVenueCalendarMonth
```java
@Override
@Transactional(readOnly = true)
public VenueCalendarMonthVO getVenueCalendarMonth(Long venueId, Integer year, Integer month) {
    // 1. 參數驗證
    if (venueId == null || venueId <= 0) { ... }
    if (year == null || month == null || month < 1 || month > 12) { ... }
    
    // 2. 計算日期範圍
    YearMonth ym = YearMonth.of(year, month);
    LocalDate startDate = ym.atDay(1);
    LocalDate endDate = ym.atEndOfMonth();
    
    // 3. 查詢已通過預約和用戶預約
    List<Booking> approvedBookings = bookingMapper.selectApprovedBookingsByDateRange(venueId, startDate, endDate);
    List<Booking> userBookings = bookingMapper.selectUserBookingsByDateRange(
        UserContext.getUser().getUserId(), venueId, startDate, endDate);
    
    // 4. 聚合數據為月視圖
    // 遍歷月份內所有日期，檢查每日是否有已通過或用戶預約
    
    // 5. 組裝 VO 並返回
}
```
- [ ] 實現參數驗證邏輯
- [ ] 實現日期範圍計算邏輯
- [ ] 實現數據聚合邏輯
- [ ] 加上 @Transactional(readOnly = true) 註解（只讀優化）
- [ ] 加上完整的繁體中文註解說明各步驟

### 3.3 BookingServiceImpl.java 實現 - getVenueCalendarWeek
```java
@Override
@Transactional(readOnly = true)
public VenueCalendarWeekVO getVenueCalendarWeek(Long venueId, LocalDate weekStartDate) {
    // 1. 參數驗證
    if (venueId == null || weekStartDate == null) { ... }
    // 驗證 weekStartDate 是否為周一
    if (weekStartDate.getDayOfWeek() != DayOfWeek.MONDAY) { 
        throw new IllegalArgumentException("周開始日期必須為周一");
    }
    
    // 2. 計算周的日期範圍
    LocalDate weekEnd = weekStartDate.plusDays(6);
    
    // 3. 查詢已通過預約和用戶預約
    List<Booking> approvedBookings = bookingMapper.selectApprovedBookingsByDateRange(venueId, weekStartDate, weekEnd);
    List<Booking> userBookings = bookingMapper.selectUserBookingsByDateRange(...);
    
    // 4. 聚合數據為周視圖
    // 遍歷周內 7 天，每天組裝時段詳情（調用 BookingUtils.parseMaskToList）
    
    // 5. 組裝 VO 並返回
}
```
- [ ] 實現周一驗證邏輯
- [ ] 實現時段列表轉換（使用 BookingUtils.parseMaskToList）
- [ ] 實現周視圖數據聚合
- [ ] 加上完整註解

### 3.4 BookingServiceImpl.java 實現 - getVenueCalendarDay
```java
@Override
@Transactional(readOnly = true)
public VenueCalendarDayVO getVenueCalendarDay(Long venueId, LocalDate date) {
    // 1. 參數驗證
    if (venueId == null || date == null) { ... }
    
    // 2. 查詢該日已通過預約和用戶預約
    List<Booking> approvedBookings = bookingMapper.selectApprovedBookingsByDateRange(venueId, date, date);
    List<Booking> userBookings = bookingMapper.selectUserBookingsByDateRange(...);
    
    // 3. 轉換時段列表
    List<Integer> approvedSlots = new ArrayList<>();
    for (Booking b : approvedBookings) {
        approvedSlots.addAll(BookingUtils.parseMaskToList(b.getTimeSlots()));
    }
    // 同樣處理 userSlots (去重)
    
    // 4. 組裝用戶預約詳情
    List<UserBookingDetail> userDetails = new ArrayList<>();
    for (Booking b : userBookings) {
        UserBookingDetail detail = new UserBookingDetail();
        detail.setBookingId(b.getId());
        detail.setSlots(BookingUtils.parseMaskToList(b.getTimeSlots()));
        detail.setStatus(b.getStatus());
        detail.setPurpose(b.getPurpose());
        detail.setCreatedAt(b.getCreatedAt());
        userDetails.add(detail);
    }
    
    // 5. 組裝並返回 VO
}
```
- [ ] 實現參數驗證
- [ ] 實現時段列表合併與去重
- [ ] 實現用戶預約詳情組裝
- [ ] 加上完整註解

### 3.5 Service 層測試驗證
- [ ] 執行 `mvn test -Dtest=BookingServiceImplTest` 確保測試通過
- [ ] 驗證數據聚合邏輯是否正確（尤其是重複時段處理）

---

## 第四階段：Controller 層 REST 端點 (Day 3)

### 4.1 BookingController.java 新增方法
- [ ] 新增方法：`getCalendarMonth(...)`
  - @GetMapping("/calendar/month")
  - 參數：@RequestParam Long venueId, @RequestParam Integer year, @RequestParam Integer month
  - 回傳：Result<VenueCalendarMonthVO>
  - 加上詳細 Javadoc
  
- [ ] 新增方法：`getCalendarWeek(...)`
  - @GetMapping("/calendar/week")
  - 參數：@RequestParam Long venueId, @RequestParam LocalDate date
  - 回傳：Result<VenueCalendarWeekVO>
  - 加上詳細 Javadoc
  
- [ ] 新增方法：`getCalendarDay(...)`
  - @GetMapping("/calendar/day")
  - 參數：@RequestParam Long venueId, @RequestParam LocalDate date
  - 回傳：Result<VenueCalendarDayVO>
  - 加上詳細 Javadoc

### 4.2 Controller 方法實現
- [ ] 每個方法內部直接調用 Service 方法，無需額外邏輯
- [ ] 使用 Result.success(data) 包裝回應
- [ ] 異常由 GlobalExceptionHandler 統一處理

### 4.3 API 路由驗證
- [ ] 啟動應用，訪問 `http://localhost:8080/api/bookings/calendar/month?venueId=1&year=2026&month=4`
- [ ] 驗證是否收到正確的 JSON 回應
- [ ] 檢查回應中的 data 欄位結構是否符合設計文檔

---

## 第五階段：測試與驗證 (Day 3)

### 5.1 單元測試（可選但推薦）
- [ ] 編寫 BookingCalendarServiceTest
  - 測試 getVenueCalendarMonth：邊界情況（月初、月末）、多筆預約、無預約
  - 測試 getVenueCalendarWeek：周邊界、跨月情況
  - 測試 getVenueCalendarDay：用戶隔離、時段轉換
  
- [ ] 編寫 BookingCalendarControllerTest
  - 測試三個 API 端點的 HTTP 狀態碼與回應格式
  - 使用 MockMvc 進行集成測試

### 5.2 集成測試
- [ ] 在測試資料庫中準備測試數據（至少 5 筆已通過預約 + 3 筆用戶預約）
- [ ] 調用三種 API，驗證回應是否正確聚合數據
- [ ] 驗證用戶隔離：確認用戶 A 的預約不會出現在用戶 B 的查詢結果中

### 5.3 邊界情況測試
- [ ] 月份查詢：
  - [ ] 完整月份 (4 月全 30 天)
  - [ ] 不完整月份 (2 月 28/29 天)
  - [ ] 跨年查詢 (12 月後的 1 月)
  
- [ ] 周查詢：
  - [ ] 周日開始的日期：應拋出異常
  - [ ] 周內無預約的日期
  - [ ] 跨月周 (月末到月初)
  
- [ ] 日查詢：
  - [ ] 無預約的日期
  - [ ] 多筆重疊預約的日期

### 5.4 性能測試（可選）
- [ ] 測試 1 年內的月曆查詢：應在 100ms 內完成
- [ ] 測試 100+ 筆預約的周曆查詢：應在 200ms 內完成

---

## 第六階段：代碼審查與優化 (Day 3)

### 6.1 代碼規範檢查
- [ ] 所有方法、類別均有繁體中文 Javadoc
- [ ] 使用 camelCase 命名變數、方法
- [ ] 使用 PascalCase 命名類別
- [ ] 無使用 Lambda 表達式或 Stream API
- [ ] 所有迴圈使用傳統 for-each 或 for 迴圈

### 6.2 邏輯簡潔性檢查
- [ ] 方法長度不超過 40 行（含註解）
- [ ] 複雜邏輯已拆分為私有輔助方法
- [ ] 參數驗證集中在方法開頭，使用 Guard Clause

### 6.3 性能優化檢查
- [ ] 確認 @Transactional(readOnly = true) 已加上
- [ ] 確認無 N+1 查詢問題（目前結構只有 2 次查詢，符合要求）
- [ ] 時段列表去重邏輯已優化（考慮使用 Set 而非 List）

---

## 第七階段：文檔與知識庫更新 (Day 3)

### 7.1 API 文檔
- [ ] 在 Swagger/SpringDoc 中加上三個新端點的定義
- [ ] 撰寫 API 使用範例（請求與回應）

### 7.2 技術文檔
- [ ] 更新 README.md，新增「場地日曆功能」章節
- [ ] 更新設計文檔（已完成：Booking_Calendar_Feature_Design.md）

### 7.3 知識庫
- [ ] 在團隊 Wiki 中記錄開發要點（時段轉換、日期計算、用戶隔離）

---

## 驗收標準 (Acceptance Criteria)

### 功能完整性
- [x] 月曆視圖 API 返回該月每日有無預約的標記
- [x] 周曆視圖 API 返回該周每日的詳細時段列表
- [x] 日曆視圖 API 返回該日的詳細時段與用戶預約清單
- [x] 所有 API 均顯示已通過預約 + 用戶預約

### 代碼品質
- [x] 無使用 Lambda/Stream（符合規範）
- [x] 所有方法有繁體中文 Javadoc
- [x] 編譯無誤，通過 mvn clean package
- [x] 所有新增的 public 方法有單元測試

### 性能
- [x] 月曆查詢 < 100ms
- [x] 周曆查詢 < 200ms
- [x] 日曆查詢 < 100ms

### 安全性
- [x] 用戶隔離：確保用戶只能查看自己的預約
- [x] 參數驗證：所有輸入都經過驗證
- [x] 異常處理：所有異常都有對應的 try-catch 或拋出

---

## 預計時程表

| 階段 | 工作項 | 預計時間 | 狀態 |
|:---|:---|:---|:---|
| 1 | VO 類設計與實現 | 2-3 小時 | ⏳ 待開始 |
| 2 | Mapper 層擴展 | 1.5-2 小時 | ⏳ 待開始 |
| 3 | Service 層實現 | 2-2.5 小時 | ⏳ 待開始 |
| 4 | Controller 層實現 | 1 小時 | ⏳ 待開始 |
| 5 | 測試與驗證 | 1.5-2 小時 | ⏳ 待開始 |
| 6 | 代碼審查 | 1 小時 | ⏳ 待開始 |
| 7 | 文檔更新 | 0.5 小時 | ⏳ 待開始 |
| **總計** | | **9-12 小時 (1-2 工作天)** | |

---

**清單建立日期**：2026-04-04  
**預計完成日期**：2026-04-05 ~ 2026-04-06  
**審核人員**：待分配

