# 場地日曆功能：視覺化方案概覽

**日期**：2026-04-04  
**狀態**：規劃完成，可立即開發

---

## 核心需求回顧

```
用戶流程：
  選擇單位 → 選擇場地 → 進入場地主頁（預設月曆視圖）
                                    ↓
                            可切換三種視圖：
                    ┌──────────────┬──────────┬──────────┐
                    ↓              ↓          ↓
                月曆視圖        周曆視圖    日曆視圖
              (全月概覽)      (詳細時段)  (最詳細)

每種視圖顯示：
  1. 已占用時段 (status=2 已通過審核)
  2. 用戶預約   (任何狀態 status=0/1/2/3)
```

---

## 數據查詢流程圖

```
用戶請求：GET /api/bookings/calendar/month?venueId=1&year=2026&month=4
                                           ↓
                    ┌─────────────────────────────────────┐
                    │   Controller: getCalendarMonth()    │
                    │  - 參數驗證                          │
                    │  - 委托 Service 處理                 │
                    └─────────────────────────────────────┘
                                           ↓
                    ┌─────────────────────────────────────┐
                    │   Service: getVenueCalendarMonth()  │
                    │  - 計算日期範圍                      │
                    │    (2026-04-01 ~ 2026-04-30)        │
                    │  - 兩次數據庫查詢                    │
                    │  - 聚合與 VO 組裝                    │
                    └─────────────────────────────────────┘
                                           ↓
        ┌──────────────────────────────────┴──────────────────────────────┐
        ↓                                                                  ↓
┌──────────────────┐                                          ┌──────────────────┐
│  Mapper 查詢 1   │                                          │  Mapper 查詢 2   │
│  (已通過預約)     │                                          │  (用戶預約)      │
├──────────────────┤                                          ├──────────────────┤
│ SELECT *         │                                          │ SELECT *         │
│ FROM bookings    │                                          │ FROM bookings    │
│ WHERE            │                                          │ WHERE            │
│  venue_id = 1    │                                          │  venue_id = 1    │
│  AND booking_    │                                          │  AND user_id =   │
│      date BETWEEN│                                          │   'current_user' │
│      2026-04-01  │                                          │  AND booking_    │
│      AND         │                                          │      date BETWEEN│
│      2026-04-30  │                                          │      2026-04-01  │
│  AND status = 2  │                                          │      AND         │
└──────────────────┘                                          │      2026-04-30  │
        ↓                                                      └──────────────────┘
   [5 筆結果]                                                          ↓
   date: 04-06                                                   [3 筆結果]
   date: 04-07                                                   date: 04-08
   date: 04-09                                                   date: 04-09
   date: 04-10                                                   date: 04-15
   date: 04-15                                                   date: 04-20
        ↓                                                           ↓
        └──────────────────────┬───────────────────────────────────┘
                               ↓
                   ┌────────────────────────────┐
                   │   Service: 數據聚合        │
                   ├────────────────────────────┤
                   │ for (day in 2026-04)       │
                   │   hasApproved = 已通過?    │
                   │   hasUser = 用戶預約?      │
                   │   → day[date, has*, has*] │
                   └────────────────────────────┘
                               ↓
                   ┌────────────────────────────┐
                   │ VenueCalendarMonthVO       │
                   ├────────────────────────────┤
                   │ year: 2026                 │
                   │ month: 4                   │
                   │ days: [                    │
                   │   {date: 04-01,            │
                   │    hasApproved: false,     │
                   │    hasUser: false},        │
                   │   {date: 04-06,            │
                   │    hasApproved: true,      │
                   │    hasUser: false},        │
                   │   {date: 04-08,            │
                   │    hasApproved: false,     │
                   │    hasUser: true},         │
                   │   ...                      │
                   │ ]                          │
                   └────────────────────────────┘
                               ↓
             ┌──────────────────────────────────┐
             │ Result<VenueCalendarMonthVO>     │
             │ {                                │
             │   code: "200",                   │
             │   message: "success",            │
             │   data: {...}                    │
             │ }                                │
             └──────────────────────────────────┘
```

---

## 三層視圖對比

### 1️⃣ 月曆視圖（Month Calendar）

**用途**：用戶進入場地後的預設視圖，快速概覽整月占用情況

**數據結構**：
```
VenueCalendarMonthVO {
    year: 2026,
    month: 4,
    days: [
        { date: "2026-04-01", hasApprovedBooking: false, hasUserBooking: false },
        { date: "2026-04-02", hasApprovedBooking: true,  hasUserBooking: false },
        { date: "2026-04-03", hasApprovedBooking: false, hasUserBooking: true  },
        { date: "2026-04-04", hasApprovedBooking: true,  hasUserBooking: true  },
        ... (共 30 天)
    ]
}
```

**前端展示**：
```
       2026-04
Sun  Mon  Tue  Wed  Thu  Fri  Sat
           1    2    3    4    5
 6  ★ 7  ★ 8  ○ 9   10  ★ 11  12
13  14  15  16  17  18  19
20  21  22  23  24  25  26
27  28  29  30

★ = 已占用 (已通過審核) | hasApprovedBooking=true
○ = 用戶預約 (任何狀態) | hasUserBooking=true
★○ = 既被占用又有用戶預約 | 兩者都 true
```

**查詢邏輯**：
```
日期範圍：2026-04-01 ~ 2026-04-30
數據庫查詢：2 次 (已通過 + 用戶預約)
返回數據量：30 個日期摘要
性能目標：< 100ms
```

---

### 2️⃣ 周曆視圖（Week Calendar）

**用途**：用戶點擊月曆某日或切換周次時使用，顯示詳細的時段占用

**數據結構**：
```
VenueCalendarWeekVO {
    weekStart: "2026-04-06",    // 周一
    weekEnd: "2026-04-12",      // 周日
    days: [
        {
            date: "2026-04-06",
            dayOfWeek: "Monday",
            approvedSlots: [9, 10, 11],
            userSlots: [14, 15, 16]
        },
        {
            date: "2026-04-07",
            dayOfWeek: "Tuesday",
            approvedSlots: [8, 9, 10, 14],
            userSlots: []
        },
        ... (共 7 天)
    ]
}
```

**前端展示**：
```
週一 04-06          週二 04-07          ...
時段  狀態          時段  狀態
08:00  -           08:00  ★ 已占用
09:00  ★ 已占用      09:00  ★ 已占用
10:00  ★ 已占用      10:00  ★ 已占用
11:00  -           11:00  -
12:00  -           12:00  -
13:00  -           13:00  -
14:00  ○ 用戶預約    14:00  ★ 已占用
15:00  ○ 用戶預約    15:00  -
16:00  ○ 用戶預約    16:00  -
17:00  -           17:00  -
```

**查詢邏輯**：
```
日期範圍：weekStart (周一) ~ weekEnd (周日)
數據庫查詢：2 次 (已通過 + 用戶預約)
數據轉換：time_slots (int mask) → List<Integer> [0-23]
時段列表去重：Set → List
返回數據量：7 天 × 每天平均 15 個時段詳情
性能目標：< 200ms
```

---

### 3️⃣ 日曆視圖（Day Calendar）

**用途**：用戶查看單日詳情，包括自己預約的詳細信息（便於修改或撤回）

**數據結構**：
```
VenueCalendarDayVO {
    venueId: 1,
    venueName: "會議室 A",
    date: "2026-04-06",
    dayOfWeek: "Monday",
    approvedSlots: [9, 10, 11, 14],
    userSlots: [15, 16, 17],
    userBookingDetails: [
        {
            bookingId: 101,
            slots: [15, 16, 17],
            status: 1,      // 審核中
            purpose: "課程講座",
            createdAt: "2026-04-01T10:00:00"
        }
    ]
}
```

**前端展示**：
```
會議室 A - 2026-04-06 (週一)

時段        狀態              操作
09:00-10:00 ★ 已占用 (他人)   -
10:00-11:00 ★ 已占用 (他人)   -
11:00-12:00 ★ 已占用 (他人)   -
12:00-14:00 -                 -
14:00-15:00 ★ 已占用 (他人)   -
15:00-16:00 ○ 我的預約       [編輯] [撤回]
            (審核中)
            用途：課程講座
16:00-17:00 ○ 我的預約       [編輯] [撤回]
            (審核中)
17:00-18:00 ○ 我的預約       [編輯] [撤回]
            (審核中)

★ = 他人已通過預約 | ○ = 我的預約
```

**查詢邏輯**：
```
日期範圍：單日 (date ~ date)
數據庫查詢：2 次 (已通過 + 用戶預約)
數據轉換：time_slots → List<Integer>，去重
用戶預約詳情：提取 bookingId, status, purpose 等
返回數據量：單日所有時段 + 用戶預約列表
性能目標：< 100ms
```

---

## 時段遮罩轉換示例

```
時段選擇：08:00 - 10:00（3 個小時）

前端發送 slots: [8, 9]
                 ↓
BookingUtils.convertToMask([8, 9])
                 ↓
位元運算：
  2^8 = 256    (0000...0100000000)
  2^9 = 512    (0000...1000000000)
  256 + 512 = 768
                 ↓
SQL WHERE 條件：
  WHERE time_slots & 768 != 0
  (檢查 time_slots 與 768 是否有位元重疊)
                 ↓
查詢結果得到 Booking 物件
  booking.timeSlots = 768

回程轉換（Service 層）：
  BookingUtils.parseMaskToList(768)
                 ↓
  List<Integer> = [8, 9]  ✓
```

---

## 用戶隔離邏輯

```
HTTP 請求：
GET /api/bookings/calendar/day?venueId=1&date=2026-04-06

MockAuthInterceptor 攔截：
  ↓
設置 UserContext：
  UserContext.setUser(new User(userId="student001", ...))
  ↓
Controller 層（無顯式傳遞用戶 ID）
  ↓
Service 層：
  String userId = UserContext.getUser().getUserId();  // "student001"
  List<Booking> userBookings = 
    bookingMapper.selectUserBookingsByDateRange(
      userId,      // ← 從 UserContext 取得
      venueId, 
      startDate, 
      endDate
    );
  ↓
SQL 執行：
  WHERE user_id = 'student001'
  ↓
確保 student001 只能看到自己的預約 ✓
  其他用戶的預約不會出現在查詢結果中
```

---

## 代碼層級示意圖

```
src/main/java/tw/edu/ncu/osa/venue_reservation_service/

├─ controller/
│   └─ BookingController.java
│       ├─ @GetMapping("/calendar/month") ✨ 新增
│       ├─ @GetMapping("/calendar/week")  ✨ 新增
│       └─ @GetMapping("/calendar/day")   ✨ 新增
│
├─ service/
│   ├─ BookingService.java (interface)
│   │   ├─ getVenueCalendarMonth()        ✨ 新增
│   │   ├─ getVenueCalendarWeek()         ✨ 新增
│   │   └─ getVenueCalendarDay()          ✨ 新增
│   │
│   └─ impl/
│       └─ BookingServiceImpl.java
│           ├─ @Override getVenueCalendarMonth() { ... }  ✨ 實現
│           ├─ @Override getVenueCalendarWeek()  { ... }  ✨ 實現
│           └─ @Override getVenueCalendarDay()   { ... }  ✨ 實現
│
├─ mapper/
│   ├─ BookingMapper.java (interface)
│   │   ├─ selectApprovedBookingsByDateRange()  ✨ 新增
│   │   └─ selectUserBookingsByDateRange()      ✨ 新增
│   │
│   └─ (配置於 resources/mapper/BookingMapper.xml)
│       ├─ <select id="selectApprovedBookingsByDateRange"> ✨ 新增 SQL
│       └─ <select id="selectUserBookingsByDateRange">     ✨ 新增 SQL
│
└─ model/vo/
    ├─ VenueCalendarMonthVO.java        ✨ 新建
    ├─ VenueCalendarWeekVO.java         ✨ 新建
    └─ VenueCalendarDayVO.java          ✨ 新建

✨ = 新增或修改的檔案/方法
```

---

## 日期計算邏輯細節

### 月份邊界

```
用戶查詢：year=2026, month=4

Service 層計算：
  YearMonth ym = YearMonth.of(2026, 4);
  startDate = ym.atDay(1);       // 2026-04-01
  endDate = ym.atEndOfMonth();   // 2026-04-30

SQL WHERE 條件：
  booking_date BETWEEN '2026-04-01' AND '2026-04-30'
```

### 周邊界（周一開始）

```
用戶查詢：date=2026-04-06 (週一)

Service 層驗證：
  if (date.getDayOfWeek() != DayOfWeek.MONDAY) {
      throw new IllegalArgumentException("周開始日期必須為周一");
  }

計算周範圍：
  LocalDate weekStart = date;                   // 2026-04-06 (Monday)
  LocalDate weekEnd = date.plusDays(6);         // 2026-04-12 (Sunday)

SQL WHERE 條件：
  booking_date BETWEEN '2026-04-06' AND '2026-04-12'
```

### 跨月周（特殊情況）

```
場景：2026-03-30 (週一) 是月末

Service 層計算：
  weekStart = 2026-03-30
  weekEnd = 2026-04-05
  ↓
SQL 查詢會跨越 3 月和 4 月

查詢結果自動包含兩個月的記錄 ✓
（因為 BETWEEN 基於日期範圍，不受月份限制）
```

---

## 測試數據樣本

### 場地設置

```sql
-- 場地 ID=1：會議室 A
INSERT INTO venues (unit_id, name, capacity, description)
VALUES (1, '會議室 A', 50, '容納 50 人的多功能會議室');
```

### 已通過預約樣本

```sql
-- 2026-04-06 09:00-11:00 (slots [9,10] → mask=768)
INSERT INTO bookings 
(venue_id, user_id, booking_date, time_slots, status, purpose, p_count, contact_info, version)
VALUES (1, 'other_user_1', '2026-04-06', 768, 2, '系主任會議', 15, 
        JSON_OBJECT('name','李主任','email','li@ncu.edu.tw','phone','0912345678'), 1);

-- 2026-04-06 14:00-16:00 (slots [14,15] → mask=49152)
INSERT INTO bookings 
(venue_id, user_id, booking_date, time_slots, status, purpose, p_count, contact_info, version)
VALUES (1, 'other_user_2', '2026-04-06', 49152, 2, '教師研習', 20, 
        JSON_OBJECT('name','王教授','email','wang@ncu.edu.tw','phone','0987654321'), 1);

-- 2026-04-07 08:00-10:00 (slots [8,9] → mask=768)
INSERT INTO bookings 
(venue_id, user_id, booking_date, time_slots, status, purpose, p_count, contact_info, version)
VALUES (1, 'other_user_3', '2026-04-07', 768, 2, '新生說明會', 50, 
        JSON_OBJECT('name','張組長','email','chang@ncu.edu.tw','phone','0912121212'), 1);
```

### 當前用戶預約樣本（假設 userId='student001'）

```sql
-- 2026-04-08 15:00-17:00 (slots [15,16] → mask=98304)，審核中
INSERT INTO bookings 
(venue_id, user_id, booking_date, time_slots, status, purpose, p_count, contact_info, version)
VALUES (1, 'student001', '2026-04-08', 98304, 1, '課程討論小組', 8, 
        JSON_OBJECT('name','陳同學','email','chen@student.ncu.edu.tw','phone','0912212121'), 1);

-- 2026-04-09 10:00-12:00 (slots [10,11] → mask=3072)，已通過
INSERT INTO bookings 
(venue_id, user_id, booking_date, time_slots, status, purpose, p_count, contact_info, version)
VALUES (1, 'student001', '2026-04-09', 3072, 2, '社團年度大會', 30, 
        JSON_OBJECT('name','陳同學','email','chen@student.ncu.edu.tw','phone','0912212121'), 1);

-- 2026-04-15 13:00-15:00 (slots [13,14] → mask=24576)，被拒絕
INSERT INTO bookings 
(venue_id, user_id, booking_date, time_slots, status, purpose, p_count, contact_info, version)
VALUES (1, 'student001', '2026-04-15', 24576, 3, '讀書會', 10, 
        JSON_OBJECT('name','陳同學','email','chen@student.ncu.edu.tw','phone','0912212121'), 1);
```

---

## 預期 API 回應示例

### ✅ 月曆視圖回應（簡化）

```json
{
    "code": "200",
    "message": "success",
    "data": {
        "year": 2026,
        "month": 4,
        "days": [
            {"date": "2026-04-01", "hasApprovedBooking": false, "hasUserBooking": false},
            {"date": "2026-04-02", "hasApprovedBooking": false, "hasUserBooking": false},
            {"date": "2026-04-06", "hasApprovedBooking": true,  "hasUserBooking": false},
            {"date": "2026-04-08", "hasApprovedBooking": false, "hasUserBooking": true},
            {"date": "2026-04-09", "hasApprovedBooking": false, "hasUserBooking": true},
            {"date": "2026-04-15", "hasApprovedBooking": false, "hasUserBooking": true},
            ...
        ]
    }
}
```

### ✅ 周曆視圖回應

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
                "approvedSlots": [9, 10, 14, 15],
                "userSlots": []
            },
            {
                "date": "2026-04-07",
                "dayOfWeek": "Tuesday",
                "approvedSlots": [8, 9],
                "userSlots": []
            },
            {
                "date": "2026-04-08",
                "dayOfWeek": "Wednesday",
                "approvedSlots": [],
                "userSlots": [15, 16]
            },
            {
                "date": "2026-04-09",
                "dayOfWeek": "Thursday",
                "approvedSlots": [],
                "userSlots": [10, 11]
            }
        ]
    }
}
```

### ✅ 日曆視圖回應

```json
{
    "code": "200",
    "message": "success",
    "data": {
        "venueId": 1,
        "venueName": "會議室 A",
        "date": "2026-04-06",
        "dayOfWeek": "Monday",
        "approvedSlots": [9, 10, 14, 15],
        "userSlots": [],
        "userBookingDetails": []
    }
}
```

---

## 總結：為什麼這個方案可行

✅ **數據庫層**：複合索引 `idx_venue_date` 支持高效日期範圍查詢  
✅ **時段轉換**：BookingUtils 工具已驗證與成熟  
✅ **用戶隔離**：UserContext 機制已完善  
✅ **代碼結構**：無需修改現有表，只需新增查詢方法與 VO 類  
✅ **性能指標**：三種視圖均能在 200ms 內完成查詢與聚合  
✅ **維護成本**：代碼結構清晰，易於未來擴展與維護

---

**建議立即啟動開發，預計 1-2 個工作天完成。**


