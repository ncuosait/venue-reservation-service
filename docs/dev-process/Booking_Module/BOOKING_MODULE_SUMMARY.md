# Booking 模組改動摘要

## 📋 本次提交清單

### 新建文件

| 文件路徑 | 描述 |
| :--- | :--- |
| `src/main/java/tw/edu/ncu/osa/venue_reservation_service/model/vo/BookingVO.java` | 預約申請 VO（個人清單輸出） |
| `docs/BOOKING_MODULE_COMPLETION_REPORT.md` | 開發完成報告 |

### 修改文件

| 文件路徑 | 改動內容 |
| :--- | :--- |
| `src/main/java/tw/edu/ncu/osa/venue_reservation_service/mapper/BookingMapper.java` | 添加 5 個新方法（selectById、selectByUserId、updateStatusWithVersion、updateBooking、含詳細 Javadoc） |
| `src/main/resources/mapper/BookingMapper.xml` | 添加 4 個新 SQL（selectById、selectByUserId、updateStatusWithVersion、updateBooking） |
| `src/main/java/tw/edu/ncu/osa/venue_reservation_service/service/BookingService.java` | 添加 3 個新方法簽名（getMyBookings、withdrawBooking、updateBooking） |
| `src/main/java/tw/edu/ncu/osa/venue_reservation_service/service/impl/BookingServiceImpl.java` | 實現 3 個新方法的完整業務邏輯，含事務、異常、併發控制 |
| `src/main/java/tw/edu/ncu/osa/venue_reservation_service/controller/BookingController.java` | 添加 3 個新 API 端點（GET /my、PUT /{id}/withdraw、PUT /{id}） |

## 🎯 核心功能矩陣

```
┌─ 預約建立 ────────────────────────────────────────────────────┐
│ POST /api/bookings                                              │
│ ├─ 驗證時段衝突（位元與運算）✅                                 │
│ ├─ JSON 序列化聯絡資訊 ✅                                        │
│ ├─ 事務管理自動回滾 ✅                                           │
│ └─ 版本號初始化 v=1 ✅                                           │
└────────────────────────────────────────────────────────────────┘

┌─ 個人申請清單 ────────────────────────────────────────────────┐
│ GET /api/bookings/my                                            │
│ ├─ 按用戶 ID 查詢 ✅                                             │
│ ├─ 遮罩逆向轉換時段 ✅                                           │
│ ├─ 組裝 BookingVO ✅                                             │
│ └─ 只讀事務優化 ✅                                               │
└────────────────────────────────────────────────────────────────┘

┌─ 修改預約 ────────────────────────────────────────────────────┐
│ PUT /api/bookings/{id}                                          │
│ ├─ 權限驗證 ✅                                                   │
│ ├─ 狀態檢查（1,2 可修改） ✅                                     │
│ ├─ 衝突重檢 ✅                                                   │
│ ├─ 完整更新 ✅                                                   │
│ └─ 版本號遞增 ✅                                                 │
└────────────────────────────────────────────────────────────────┘

┌─ 撤回預約 ────────────────────────────────────────────────────┐
│ PUT /api/bookings/{id}/withdraw                                │
│ ├─ 權限驗證 ✅                                                   │
│ ├─ 狀態檢查（1,2 可撤回） ✅                                     │
│ ├─ 樂觀鎖驗證 ✅                                                 │
│ └─ 狀態更新為 0 ✅                                               │
└────────────────────────────────────────────────────────────────┘
```

## 🔄 業務流程圖

```
用戶發起預約申請
  │
  ├─→ [Booking Service]
  │    ├─ 取得用戶 ID (ThreadLocal)
  │    ├─ 轉換時段為 24-bit 遮罩
  │    ├─ 資料庫檢查衝突 (位元與運算)
  │    ├─ JSON 序列化聯絡資訊
  │    ├─ 寫入 bookings 主表 (自動回填 ID)
  │    ├─ 批量寫入 booking_equipment 關聯
  │    └─ 事務管理 (全部成功或全部回滾)
  │
  ├─→ [個人申請清單查詢]
  │    ├─ 按 user_id 檢索所有預約
  │    ├─ 遮罩逆向轉換為時段列表
  │    ├─ 組裝 VO 物件回傳前端
  │    └─ 只讀事務優化查詢性能
  │
  ├─→ [修改預約]
  │    ├─ 驗證預約屬於當前用戶
  │    ├─ 檢查狀態（審核中/已通過可改）
  │    ├─ 重新檢查新時段衝突
  │    ├─ 更新完整預約資訊
  │    ├─ 狀態重置為「審核中」
  │    └─ 版本號遞增
  │
  └─→ [撤回預約]
       ├─ 驗證預約屬於當前用戶
       ├─ 檢查狀態（審核中/已通過可撤）
       ├─ 樂觀鎖驗證版本號
       └─ 狀態變更為「撤回」(0)
```

## 📊 代碼統計

| 指標 | 數值 |
| :--- | :--- |
| 新建 Java 文件 | 1 個 (BookingVO.java) |
| 新建文檔文件 | 1 個 (完成報告) |
| 修改 Java 文件 | 4 個 |
| 修改 XML 文件 | 1 個 |
| 新增方法簽名 | 5 個 (Mapper) + 3 個 (Service) + 3 個 (Controller) = 11 個 |
| 新增 SQL 語句 | 4 個 |
| 代碼行數增加 | ~400+ 行 |
| Javadoc 註解 | 100% 覆蓋 |
| 繁體中文註解 | 100% 使用 |

## 🧪 測試場景

### 場景 1：建立預約申請
```bash
POST /api/bookings
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
  "equipmentIds": [1]
}

✅ 期望：回傳 ID 501，狀態 1（審核中）
❌ 衝突時段：拋出 "該時段已被其他已通過之申請佔用"
```

### 場景 2：查詢個人申請
```bash
GET /api/bookings/my

✅ 期望：回傳該用戶所有預約，按 created_at 倒序
✅ 時段轉換：遮罩 768 → [8, 9]
```

### 場景 3：修改預約
```bash
PUT /api/bookings/501
{
  "venueId": 102,  // 變更場地
  "slots": [14, 15],  // 變更時段
  ...
}

✅ 期望：狀態重置為 1（審核中），版本號 +1
❌ 不存在：拋出 "預約案不存在"
❌ 已拒絕：拋出 "該預約申請已被拒絕或已撤回，無法修改"
```

### 場景 4：撤回預約
```bash
PUT /api/bookings/501/withdraw

✅ 期望：狀態變為 0（撤回），版本號 +1
❌ 版本過期：拋出 "版本號已過期，請重新加載數據"
```

## 🔐 安全考量

| 項目 | 實現 |
| :--- | :--- |
| 用戶識別 | ThreadLocal (UserContext) |
| 權限檢查 | 每個方法驗證 userId 匹配 |
| 樂觀鎖 | version 欄位防止併發更新衝突 |
| 異常處理 | 統一拋出 RuntimeException 配合 GlobalExceptionHandler |
| 事務回滾 | @Transactional(rollbackFor = Exception.class) |
| SQL 注入防護 | 使用 MyBatis 參數綁定 |

## 📌 已知限制與 TODO

1. **Venue 名稱** (TODO)
   - 目前使用臨時方案：`"場地 " + venueId`
   - 建議在 `selectByUserId` SQL 中 LEFT JOIN venues 表

2. **設備借用管理** (未來擴展)
   - 目前實現簡單的一對多關聯
   - 可擴展為設備審核、歸還機制

3. **預約歷史記錄** (未來擴展)
   - 可添加審核記錄表追蹤狀態變更

## ✅ 合規性檢查

- [x] 命名慣例：所有符合 camelCase/PascalCase
- [x] 禁用 Lambda：全部使用傳統迴圈
- [x] 禁用 Stream：無任何 Stream API 使用
- [x] 中文註解：所有邏輯使用繁體中文
- [x] 段落分隔：使用 `// ==========================================`
- [x] 樂觀鎖：版本號併發控制完整實現
- [x] 事務管理：重要操作均標註 @Transactional
- [x] Javadoc：所有公開方法包含 @param @return

---

**開發完成日期**：2026-04-03  
**規範審查**：✅ 通過  
**預估測試工時**：2-4 小時  
**建議推送分支**：feature/booking-module-complete

