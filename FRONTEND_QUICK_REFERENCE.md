# 前端開發者快速參考卡

**日期**：2026-04-04  
**用途**：快速查詢 API 信息，無需查閱完整文檔

---

## 🚀 API 端點速查表

### Module 1：場地和基礎數據

| 功能 | 方法 | 端點 | 參數 |
|:---|:---|:---|:---|
| 取得單位列表 | GET | `/api/public/units` | - |
| 取得場地列表 | GET | `/api/public/venues` | `unitId` |
| 取得場地詳情 | GET | `/api/public/venues/{id}` | - |

### Module 2：預約操作

| 功能 | 方法 | 端點 | 參數/Body |
|:---|:---|:---|:---|
| 提交預約 | POST | `/api/bookings` | JSON Body |
| 個人預約清單 | GET | `/api/bookings/my` | - |
| 修改預約 | PUT | `/api/bookings/{id}` | JSON Body |
| 撤回預約 | PUT | `/api/bookings/{id}/withdraw` | - |

### Module 3：日曆視圖（✨新增）

| 功能 | 方法 | 端點 | 參數 |
|:---|:---|:---|:---|
| 月曆視圖 | GET | `/api/bookings/calendar/month` | `venueId`, `year`, `month` |
| 周曆視圖 | GET | `/api/bookings/calendar/week` | `venueId`, `date` |
| 日曆視圖 | GET | `/api/bookings/calendar/day` | `venueId`, `date` |

---

## 📦 常用請求範例

### 1. 取得單位列表

```bash
curl -X GET "http://localhost:8080/api/public/units"
```

**回應：**
```json
{
  "code": "200",
  "message": "操作成功",
  "data": [
    {"id": 1, "name": "學務處本部", "code": "STUA"},
    {"id": 2, "name": "住宿服務組", "code": "HSD"}
  ]
}
```

### 2. 根據單位取得場地

```bash
curl -X GET "http://localhost:8080/api/public/venues?unitId=1"
```

### 3. 提交預約申請

```bash
curl -X POST "http://localhost:8080/api/bookings" \
  -H "Content-Type: application/json" \
  -d '{
    "venueId": 101,
    "bookingDate": "2026-04-06",
    "slots": [8, 9, 10],
    "purpose": "課程討論",
    "participantCount": 15,
    "contactInfo": {
      "name": "王小明",
      "email": "student@ncu.edu.tw",
      "phone": "0912345678"
    },
    "equipmentIds": [1, 2]
  }'
```

### 4. 查看個人預約

```bash
curl -X GET "http://localhost:8080/api/bookings/my"
```

### 5. 查看月曆

```bash
curl -X GET "http://localhost:8080/api/bookings/calendar/month?venueId=101&year=2026&month=4"
```

**回應：**
```json
{
  "code": "200",
  "message": "操作成功",
  "data": {
    "year": 2026,
    "month": 4,
    "days": [
      {"date": "2026-04-01", "hasApprovedBooking": false, "hasUserBooking": false},
      {"date": "2026-04-06", "hasApprovedBooking": true, "hasUserBooking": false}
    ]
  }
}
```

### 6. 查看周曆

```bash
curl -X GET "http://localhost:8080/api/bookings/calendar/week?venueId=101&date=2026-04-06"
```

**回應：**
```json
{
  "code": "200",
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
      }
    ]
  }
}
```

### 7. 查看日曆

```bash
curl -X GET "http://localhost:8080/api/bookings/calendar/day?venueId=101&date=2026-04-06"
```

**回應：**
```json
{
  "code": "200",
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

---

## ⏰ 時段編碼表

時段使用 0-23 的整數表示小時：

| 時段 | 編碼 | 時間 |
|:---|:---|:---|
| 上午 12-1 | `[0]` | 00:00-01:00 |
| 上午 8-9 | `[8]` | 08:00-09:00 |
| 上午 8-11 | `[8, 9, 10]` | 08:00-11:00 |
| 下午 2-5 | `[14, 15, 16]` | 14:00-17:00 |
| 全天 | `[0,1,2,...,23]` | 00:00-24:00 |

---

## 📅 日期格式

所有日期統一使用 ISO 8601 格式：

```
日期：     YYYY-MM-DD   (例如: 2026-04-06)
日期時間： YYYY-MM-DDTHH:mm:ss  (例如: 2026-04-01T10:00:00)
```

---

## 🎯 預約狀態碼

| 狀態碼 | 狀態 | 說明 |
|:---|:---|:---|
| 0 | 撤回 | 用戶撤回了預約 |
| 1 | 審核中 | 等待管理員審核 |
| 2 | 已通過 | 管理員已批准 |
| 3 | 已拒絕 | 管理員已拒絕 |

---

## 🔄 JavaScript/Axios 使用範例

### 統一 API 工具類

```javascript
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

// 創建 axios 實例
const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  }
});

// 統一響應處理
api.interceptors.response.use(
  response => {
    const { code, message, data } = response.data;
    if (code === '200') {
      return data;
    } else {
      throw new Error(message);
    }
  },
  error => {
    console.error('API 錯誤:', error.message);
    throw error;
  }
);

// 導出 API 方法
export const apiService = {
  // Module 1：場地
  getUnits: () => api.get('/public/units'),
  getVenues: (unitId) => api.get('/public/venues', { params: { unitId } }),
  getVenueDetail: (id) => api.get(`/public/venues/${id}`),

  // Module 2：預約
  createBooking: (data) => api.post('/bookings', data),
  getMyBookings: () => api.get('/bookings/my'),
  updateBooking: (id, data) => api.put(`/bookings/${id}`, data),
  withdrawBooking: (id) => api.put(`/bookings/${id}/withdraw`),

  // Module 3：日曆視圖
  getMonthCalendar: (venueId, year, month) => 
    api.get('/bookings/calendar/month', { params: { venueId, year, month } }),
  getWeekCalendar: (venueId, date) => 
    api.get('/bookings/calendar/week', { params: { venueId, date } }),
  getDayCalendar: (venueId, date) => 
    api.get('/bookings/calendar/day', { params: { venueId, date } }),
};
```

### 使用範例

```javascript
import { apiService } from './apiService';

// 取得單位列表
const units = await apiService.getUnits();

// 取得場地列表
const venues = await apiService.getVenues(1);

// 提交預約
const result = await apiService.createBooking({
  venueId: 101,
  bookingDate: '2026-04-06',
  slots: [8, 9, 10],
  purpose: '課程討論',
  participantCount: 15,
  contactInfo: {
    name: '王小明',
    email: 'student@ncu.edu.tw',
    phone: '0912345678'
  },
  equipmentIds: [1, 2]
});

// 查看月曆
const calendar = await apiService.getMonthCalendar(101, 2026, 4);
```

---

## ❌ 常見錯誤與解決

| 錯誤信息 | 原因 | 解決方案 |
|:---|:---|:---|
| 400 - 缺少必填參數 | Query 參數遺漏 | 檢查 URL 的查詢參數 |
| 404 - 場地不存在 | 場地 ID 無效 | 先取得有效的場地 ID |
| 400 - 時段衝突 | 選擇的時段已被預約 | 查看日曆視圖選擇可用時段 |
| 400 - 無權限修改 | 修改他人預約 | 確認是修改自己的預約 |
| 400 - 周開始日期必須為周一 | 日期不是周一 | 確保傳入的日期是周一 |

---

## 🎓 前端開發工作流

```
1. 取得單位列表
   ↓
2. 根據單位取得場地
   ↓
3. 用戶選擇場地
   ↓
4. 查看月曆（了解占用情況）
   ↓
5. 用戶選擇日期
   ↓
6. 查看周曆或日曆（查看詳細時段）
   ↓
7. 用戶選擇時段並填寫預約信息
   ↓
8. 提交預約申請
   ↓
9. 查看個人預約清單
   ↓
10. 修改或撤回預約（可選）
```

---

## 📞 技術支援

遇到問題時，按以下順序查詢：

1. 本快速參考卡（你現在看的）
2. `docs/Design/Overview/API_Design.md`（完整 API 文檔）
3. `docs/dev-process/Booking_Module/` 下的開發指南

---

**最後更新**：2026-04-04  
**版本**：V1.0  
**適用於**：前端開發人員


