# 場地月曆 API 文檔

## 端點資訊

### 獲取場地月曆視圖

**端點**：`GET /api/bookings/calendar/month`

**功能**：查詢指定場地指定月份的日曆視圖，包含該月所有預約資訊供前端展示

---

## 請求參數

| 參數名稱 | 類型 | 必填 | 說明 | 範例 |
|---------|------|------|------|------|
| `venueId` | Long | ✓ | 場地 ID | `1` |
| `year` | Integer | ✓ | 年份 | `2026` |
| `month` | Integer | ✓ | 月份 (1-12) | `4` |

**請求範例**：
```
GET /api/bookings/calendar/month?venueId=1&year=2026&month=4
```

---

## 回應格式

### 成功回應 (200 OK)

```json
{
  "code": 0,
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
        "date": "2026-04-02",
        "hasApprovedBooking": true,
        "hasUserBooking": false
      },
      ...
    ],
    "bookings": [
      {
        "id": 1,
        "venueName": "會議室A",
        "bookingDate": "2026-04-02",
        "slots": [8, 9, 10],
        "status": 2,
        "createdAt": "2026-04-01T10:30:00",
        "purpose": "部門例會",
        "pCount": 15,
        "contactInfo": "{\"name\":\"李明\",\"phone\":\"0912345678\",\"email\":\"liming@ncu.edu.tw\"}",
        "equipments": ["麥克風", "投影機"]
      },
      {
        "id": 2,
        "venueName": "會議室A",
        "bookingDate": "2026-04-05",
        "slots": [14, 15, 16, 17],
        "status": 1,
        "createdAt": "2026-04-02T14:15:00",
        "purpose": "專案討論",
        "pCount": 8,
        "contactInfo": "{\"name\":\"王芬\",\"phone\":\"0923456789\",\"email\":\"wangfen@ncu.edu.tw\"}",
        "equipments": ["筆記本電腦", "音響"]
      },
      {
        "id": 3,
        "venueName": "會議室A",
        "bookingDate": "2026-04-10",
        "slots": [8, 9, 10, 11, 12],
        "status": 2,
        "createdAt": "2026-03-30T09:00:00",
        "purpose": "年度規劃會議",
        "pCount": 25,
        "contactInfo": "{\"name\":\"陳河\",\"phone\":\"0934567890\",\"email\":\"chenhe@ncu.edu.tw\"}",
        "equipments": ["投影機", "麥克風", "音響"]
      }
    ]
  }
}
```

### 回應欄位說明

#### 一級物件 (VenueCalendarMonthVO)

| 欄位名稱 | 類型 | 說明 |
|---------|------|------|
| `year` | Integer | 年份 |
| `month` | Integer | 月份 (1-12) |
| `days` | List<DaySimpleSummary> | 該月每日的摘要資訊陣列 |
| `bookings` | List<BookingVO> | 該月所有預約資訊陣列（**關鍵欄位**） |

#### 日期摘要物件 (DaySimpleSummary)

| 欄位名稱 | 類型 | 說明 |
|---------|------|------|
| `date` | String | ISO 8601 日期格式 (例如: `2026-04-01`) |
| `hasApprovedBooking` | Boolean | 該日是否有已通過審核的預約 (status=2) |
| `hasUserBooking` | Boolean | 該日是否有用戶自己的預約（前端根據登入用戶ID判斷） |

#### 預約物件 (BookingVO) - **前端時段占用判斷的主要資料來源**

| 欄位名稱 | 類型 | 說明 | 備註 |
|---------|------|------|------|
| `id` | Long | 預約案編號 | 用於後續查詢預約詳情或修改操作 |
| `venueName` | String | 場地名稱 | 該預約申請的目標場地 |
| `bookingDate` | LocalDate | 預約日期 | ISO 8601 日期格式 (例如: `2026-04-02`) |
| `slots` | List<Integer> | 預約時段清單 | **時段號碼 0-23，代表 0:00-1:00, 1:00-2:00, ..., 23:00-24:00**<br/>例如 `[8, 9, 10]` 表示 08:00-11:00 時段 |
| `status` | Integer | 預約狀態 | **0**: 已撤回<br/>**1**: 審核中<br/>**2**: 已通過<br/>**3**: 已拒絕 |
| `createdAt` | LocalDateTime | 預約建立時間 | ISO 8601 格式 (例如: `2026-04-01T10:30:00`) |
| `purpose` | String | 使用用途 | 場地預約的目的說明 (例如: `部門例會`, `專案討論`) |
| `pCount` | Integer | 預估參與人數 | 參與預約活動的預計人數 |
| `contactInfo` | String | 聯絡人資訊 | **JSON 格式字串**，包含以下子欄位：<br/>- `name`: 聯絡人姓名<br/>- `phone`: 聯絡電話<br/>- `email`: 聯絡郵箱<br/>例如: `{"name":"李明","phone":"0912345678","email":"liming@ncu.edu.tw"}` |
| `equipments` | List<String> | 借用的設備清單 | 所借用的設備名稱陣列<br/>例如: `["麥克風", "投影機", "音響"]`<br/>若無借用設備則為空陣列 `[]` |

---

## 前端時段占用判斷邏輯

根據 `bookings` 陣列中各預約的 `status` 和 `slots` 判斷時段占用情況：

### 1. 已占用時段 (已通過審核)
- **條件**：`status === 2` (已通過)
- **說明**：該時段已被核准，場地不可再預約
- **顯示方式**：標記為「已占用」(紅色或禁用狀態)

```javascript
const occupiedSlots = bookings
  .filter(b => b.status === 2)
  .flatMap(b => b.slots);
```

### 2. 待審核時段 (審核中)
- **條件**：`status === 1` (審核中)
- **說明**：該時段申請已提交，正在等待審核結果
- **顯示方式**：標記為「待審核」(黃色或灰色)
- **是否禁用預約**：建議禁用，避免重複申請同一時段

```javascript
const pendingSlots = bookings
  .filter(b => b.status === 1)
  .flatMap(b => b.slots);
```

### 3. 已拒絕時段 (拒絕)
- **條件**：`status === 3` (已拒絕)
- **說明**：該時段申請已被駁回，該預約無效
- **顯示方式**：視作「未占用」，用戶可重新申請

```javascript
const rejectedSlots = bookings
  .filter(b => b.status === 3)
  .flatMap(b => b.slots);
```

### 4. 已撤回時段 (撤回)
- **條件**：`status === 0` (已撤回)
- **說明**：該時段申請已被用戶撤回，視作未占用
- **顯示方式**：視作「未占用」

```javascript
const withdrawnSlots = bookings
  .filter(b => b.status === 0)
  .flatMap(b => b.slots);
```

### 前端完整實現範例 (JavaScript/TypeScript)

```javascript
// 解析 contactInfo JSON 字串
function parseContactInfo(contactInfoStr) {
  try {
    return JSON.parse(contactInfoStr);
  } catch (error) {
    console.error('聯絡人資訊解析失敗:', error);
    return { name: '未知', phone: '', email: '' };
  }
}

// 獲取某一日期的占用時段
function getOccupiedSlots(date, bookings) {
  return bookings
    .filter(b => b.bookingDate === date && b.status === 2)
    .flatMap(b => b.slots);
}

// 獲取某一日期的待審核時段
function getPendingSlots(date, bookings) {
  return bookings
    .filter(b => b.bookingDate === date && b.status === 1)
    .flatMap(b => b.slots);
}

// 判斷某一時段是否可預約
function isSlotAvailable(date, slot, bookings) {
  // 已通過的預約時段不可再預約
  const occupiedSlots = getOccupiedSlots(date, bookings);
  if (occupiedSlots.includes(slot)) return false;
  
  // 待審核時段可選配置為不可預約（建議）
  const pendingSlots = getPendingSlots(date, bookings);
  if (pendingSlots.includes(slot)) return false;
  
  return true;
}

// 在日曆視圖中顯示時段狀態和預約詳情
function renderDaySlots(date, bookings) {
  const dayBookings = bookings.filter(b => b.bookingDate === date);
  
  const slotStatus = {};
  for (let slot = 0; slot < 24; slot++) {
    const booking = dayBookings.find(b => b.slots.includes(slot));
    
    if (!booking) {
      slotStatus[slot] = 'available';
    } else if (booking.status === 2) {
      slotStatus[slot] = {
        status: 'occupied',
        bookingId: booking.id,
        purpose: booking.purpose,
        contactName: parseContactInfo(booking.contactInfo).name
      };
    } else if (booking.status === 1) {
      slotStatus[slot] = {
        status: 'pending',
        bookingId: booking.id,
        purpose: booking.purpose
      };
    } else if (booking.status === 3) {
      slotStatus[slot] = 'available'; // 拒絕的預約視為未占用
    } else if (booking.status === 0) {
      slotStatus[slot] = 'available'; // 撤回的預約視為未占用
    }
  }
  
  return slotStatus;
}

// 顯示預約詳細資訊（包括聯絡人和設備）
function showBookingDetails(bookingId, bookings) {
  const booking = bookings.find(b => b.id === bookingId);
  if (!booking) return null;
  
  const contactInfo = parseContactInfo(booking.contactInfo);
  return {
    id: booking.id,
    date: booking.bookingDate,
    slots: booking.slots.join(','),
    purpose: booking.purpose,
    participantCount: booking.pCount,
    status: booking.status,
    contact: {
      name: contactInfo.name,
      phone: contactInfo.phone,
      email: contactInfo.email
    },
    equipments: booking.equipments || []
  };
}
```

---

## 錯誤回應

### 場地 ID 無效 (400 Bad Request)

```json
{
  "code": -1,
  "message": "場地 ID 不可為空或為負數",
  "data": null
}
```

### 年月份格式不正確 (400 Bad Request)

```json
{
  "code": -1,
  "message": "年份或月份格式不正確",
  "data": null
}
```

### 伺服器內部錯誤 (500 Internal Server Error)

```json
{
  "code": -1,
  "message": "伺服器處理請求時發生錯誤",
  "data": null
}
```

---

## 使用範例

### cURL 範例

```bash
curl -X GET "http://localhost:8080/api/bookings/calendar/month?venueId=1&year=2026&month=4" \
  -H "Content-Type: application/json"
```

### JavaScript Fetch 範例

```javascript
const venueId = 1;
const year = 2026;
const month = 4;

fetch(`/api/bookings/calendar/month?venueId=${venueId}&year=${year}&month=${month}`)
  .then(response => response.json())
  .then(result => {
    if (result.code === 0) {
      const { year, month, days, bookings } = result.data;
      console.log(`${year}年${month}月的日曆視圖`);
      console.log(`共 ${days.length} 天，${bookings.length} 筆預約記錄`);
      
      // 遍歷預約，判斷每天的時段占用
      bookings.forEach(booking => {
        console.log(`${booking.bookingDate}: 時段 ${booking.slots.join(',')} - 狀態: ${booking.status}`);
      });
    } else {
      console.error('查詢失敗:', result.message);
    }
  })
  .catch(error => console.error('請求失敗:', error));
```

### React 元件範例

```jsx
import React, { useState, useEffect } from 'react';

function VenueCalendarMonth({ venueId, year, month }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetch(`/api/bookings/calendar/month?venueId=${venueId}&year=${year}&month=${month}`)
      .then(res => res.json())
      .then(result => {
        if (result.code === 0) {
          setData(result.data);
        } else {
          setError(result.message);
        }
      })
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));
  }, [venueId, year, month]);

  // 解析聯絡人資訊
  const parseContact = (contactInfoStr) => {
    try {
      return JSON.parse(contactInfoStr);
    } catch (e) {
      return { name: '未知', phone: '', email: '' };
    }
  };

  if (loading) return <div>載入中...</div>;
  if (error) return <div>錯誤: {error}</div>;
  if (!data) return <div>無資料</div>;

  return (
    <div>
      <h2>{data.year}年{data.month}月 - 場地月曆視圖</h2>
      <div className="calendar-grid">
        {data.days.map(day => {
          const dayBookings = data.bookings.filter(b => b.bookingDate === day.date);
          return (
            <div key={day.date} className="day-cell">
              <div className="date">{day.date}</div>
              <div className="bookings">
                {dayBookings.length > 0 ? (
                  <ul>
                    {dayBookings.map(b => {
                      const contact = parseContact(b.contactInfo);
                      return (
                        <li key={b.id} className={`status-${b.status}`}>
                          <div>時段: {b.slots.join(',')}</div>
                          <div>用途: {b.purpose}</div>
                          <div>人數: {b.pCount}人</div>
                          <div>聯絡人: {contact.name} ({contact.phone})</div>
                          <div>設備: {b.equipments && b.equipments.length > 0 ? b.equipments.join(', ') : '無'}</div>
                          <div>狀態: {b.status}</div>
                        </li>
                      );
                    })}
                  </ul>
                ) : (
                  <p>無預約</p>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default VenueCalendarMonth;
```

---

## 資料量級說明

- **月份最多預約數**：約 60-100 筆（假設每天平均 2-3 筆預約）
- **API 回應體積**：約 15-20 KB JSON（包含 purpose、pCount、contactInfo 等新增欄位）
- **查詢效能**：單次資料庫查詢，毫秒級回應
- **前端處理**：簡單迴圈遍歷加 JSON 解析，無額外計算開銷

---

## 注意事項

1. **時段編號範圍**：0-23，共 24 個時段，代表每小時
2. **時段合併顯示**：前端應將連續時段視為一個時間區間顯示 (例如 [8,9,10] → 08:00-11:00)
3. **狀態優先級**：當同一時段有多個預約時，優先顯示已通過 (status=2) 的狀態
4. **向後相容**：`days` 欄位仍保留，用於快速判斷「該日是否有預約」；`bookings` 欄位是新增的詳細資訊來源
5. **前端邏輯獨立**：時段占用判斷由前端完全控制，後端無需預先計算合併

---

## 版本歷史

| 版本 | 日期 | 說明 |
|------|------|------|
| v1.2 | 2026-04-05 | 新增 `equipments` 欄位，返回每筆預約所借用的設備清單 |
| v1.1 | 2026-04-04 | 新增 `purpose`、`pCount`、`contactInfo` 欄位，提供完整的預約申請人資訊 |
| v1.0 | 2026-04-04 | 初版發布，新增 `bookings` 欄位用於返回月份內所有預約資訊 |

