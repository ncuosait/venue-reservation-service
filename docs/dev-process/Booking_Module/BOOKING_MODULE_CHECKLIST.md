# Booking Module 實施檢查清單

## ✅ 第一階段：Entity 與 DTO 層

- [x] **Booking.java** - Entity 實體類
  - [x] id（申請案編號）
  - [x] venueId（場地 ID）
  - [x] userId（申請人 ID）
  - [x] bookingDate（預約日期）
  - [x] timeSlots（24-bit 遮罩）
  - [x] status（狀態：0-3）
  - [x] purpose（使用用途）
  - [x] pCount（預估人數）
  - [x] contactInfo（聯絡資訊 JSON）
  - [x] version（樂觀鎖版本）
  - [x] createdAt/updatedAt（時間戳）

- [x] **BookingRequestDTO.java** - 請求 DTO
  - [x] venueId（必填）
  - [x] bookingDate（必填，未來日期）
  - [x] slots（必填，0-23）
  - [x] purpose（必填）
  - [x] participantCount（必填，≥1）
  - [x] contactInfo（必填）
  - [x] equipmentIds（可選）
  - [x] 完整的 @NotNull/@NotEmpty/@Min/@Max 驗證

- [x] **BookingVO.java** - 回傳 VO
  - [x] id
  - [x] venueName
  - [x] bookingDate
  - [x] slots（已轉換的清單）
  - [x] status
  - [x] createdAt

## ✅ 第二階段：Mapper 層

- [x] **BookingMapper.java** 接口
  - [x] countConflictingApprovedBookings(venueId, date, mask)
  - [x] insertBooking(booking)
  - [x] selectById(id)
  - [x] selectByUserId(userId)
  - [x] updateStatusWithVersion(id, newStatus, oldVersion)
  - [x] updateBooking(booking)
  - [x] insertBookingEquipment(bookingId, equipmentId)
  - [x] 所有方法含 Javadoc

- [x] **BookingMapper.xml**
  - [x] countConflictingApprovedBookings SQL
    - [x] 使用位元與運算 `(time_slots & #{mask}) != 0`
    - [x] 僅檢查 status = 2 的已通過案件
  - [x] insertBooking SQL
    - [x] useGeneratedKeys="true" 自動回填 ID
    - [x] version 初始化為 1
  - [x] selectById SQL
    - [x] 完整欄位查詢
  - [x] selectByUserId SQL
    - [x] 按 created_at DESC 排序
  - [x] updateStatusWithVersion SQL
    - [x] version = version + 1 遞增
    - [x] WHERE 條件含版本號驗證
  - [x] updateBooking SQL
    - [x] 完整欄位更新
    - [x] version 遞增
  - [x] insertBookingEquipment SQL

## ✅ 第三階段：Service 層

- [x] **BookingService.java** 接口
  - [x] createBooking(request) → Long
  - [x] getMyBookings() → List<BookingVO>
  - [x] updateBooking(id, request) → void
  - [x] withdrawBooking(id) → void
  - [x] 所有方法含完整 Javadoc

- [x] **BookingServiceImpl.java** 實現
  - [x] createBooking
    - [x] 從 UserContext 取得 userId
    - [x] 轉換 List<Integer> slots 為 24-bit 遮罩
    - [x] 調用 Mapper 檢查衝突
    - [x] JSON 序列化 contactInfo
    - [x] 事務管理
    - [x] 插入 bookings 與 booking_equipment
    - [x] 異常拋出
    - [x] 日誌記錄

  - [x] getMyBookings
    - [x] 從 UserContext 取得 userId
    - [x] 按 userId 查詢所有預約
    - [x] 將 timeSlots 逆向轉為 List<Integer>
    - [x] 組裝 BookingVO 清單
    - [x] 只讀事務優化
    - [x] 場地名稱臨時方案（TODO 註記）

  - [x] updateBooking
    - [x] 驗證預約案存在性
    - [x] 驗證用戶權限（userId 匹配）
    - [x] 驗證狀態可修改（1 或 2）
    - [x] 重新檢查時段衝突
    - [x] 更新完整預約資訊
    - [x] 狀態重置為 1（審核中）
    - [x] 版本號處理
    - [x] 事務管理
    - [x] 異常拋出

  - [x] withdrawBooking
    - [x] 驗證預約案存在性
    - [x] 驗證用戶權限
    - [x] 驗證狀態可撤回（1 或 2）
    - [x] 樂觀鎖版本號驗證
    - [x] 狀態更新為 0（撤回）
    - [x] 異常拋出
    - [x] 事務管理

## ✅ 第四階段：Controller 層

- [x] **BookingController.java**
  - [x] POST /api/bookings → createBooking
    - [x] @Valid @RequestBody BookingRequestDTO
    - [x] 回傳 Result<Long>
    - [x] Javadoc

  - [x] GET /api/bookings/my → getMyBookings
    - [x] 無參數
    - [x] 回傳 Result<List<BookingVO>>
    - [x] Javadoc

  - [x] PUT /api/bookings/{id} → updateBooking
    - [x] @PathVariable Long id
    - [x] @Valid @RequestBody BookingRequestDTO
    - [x] 回傳 Result<Void>
    - [x] Javadoc

  - [x] PUT /api/bookings/{id}/withdraw → withdrawBooking
    - [x] @PathVariable Long id
    - [x] 無請求體
    - [x] 回傳 Result<Void>
    - [x] Javadoc

  - [x] 所有方法使用 Result.success() 統一回傳格式
  - [x] 異常由 GlobalExceptionHandler 統一處理

## ✅ 第五階段：工具類

- [x] **BookingUtils.java**
  - [x] convertToMask(List<Integer>) 存在
  - [x] parseMaskToList(int) 存在
  - [x] isConflict(int, int) 存在

## ✅ 第六階段：代碼規範

### 命名慣例
- [x] 類別 PascalCase (BookingServiceImpl, BookingVO)
- [x] 方法 camelCase (createBooking, getMyBookings)
- [x] 變數 camelCase (userId, bookingId)
- [x] 常數大寫 (版本號初始值等)
- [x] 無模糊縮寫

### 語法與可讀性
- [x] 無 Lambda 表達式
- [x] 無 Stream API
- [x] 全部使用傳統迴圈 (for, for-each)
- [x] 方法不超過 30 行
- [x] Guard Clauses 提早回傳/拋出異常

### 註解與文件
- [x] 繁體中文註解 100%
- [x] 段落分隔符 `// ==========================================`
- [x] Javadoc 包含 @param @return
- [x] 類別層級註解說明功能

### 穩定性與架構
- [x] 樂觀鎖實現 (version 欄位)
- [x] 事務管理 (@Transactional)
- [x] 異常統一處理 (RuntimeException)
- [x] Entity/DTO/VO 職責分明
- [x] 依賴注入 (@RequiredArgsConstructor)
- [x] 資料庫層級位元運算

### 資料庫操作
- [x] 複雜 SQL 在 XML 中
- [x] 位元與運算 `&` 做衝突判定
- [x] 自動增長 ID 回填
- [x] 版本號遞增

## ✅ 第七階段：文件化

- [x] API 設計文檔已存在：`Booking_API_Design.md`
- [x] Service 設計文檔已存在：`Booking_Service_Design.md`
- [x] Mapper 設計文檔已存在：`Booking_Mapper_Design.md`
- [x] 完成報告：`BOOKING_MODULE_COMPLETION_REPORT.md`
- [x] 改動摘要：`BOOKING_MODULE_SUMMARY.md`

## 📊 最終統計

| 類別 | 數量 | 狀態 |
| :--- | :--- | :--- |
| 新建 Java 類 | 1 (BookingVO) | ✅ |
| 修改 Java 類 | 4 | ✅ |
| SQL 操作 | 7 個 | ✅ |
| API 端點 | 4 個 | ✅ |
| Service 方法 | 4 個 | ✅ |
| 文檔文件 | 2 個 | ✅ |

## 🎯 功能完整性

```
建立預約申請 ...................... ✅ 100%
查詢個人申請清單 .................. ✅ 100%
修改預約申請 ...................... ✅ 100%
撤回預約申請 ...................... ✅ 100%
────────────────────────────────────
總體完成度 ........................ ✅ 100%
```

## 🔐 安全性檢查

| 項目 | 檢查結果 |
| :--- | :--- |
| 用戶識別驗證 | ✅ ThreadLocal 每個方法驗證 |
| 權限檢查 | ✅ userId 匹配檢查 |
| 狀態校驗 | ✅ 狀態值驗證 1/2/3/0 |
| 樂觀鎖 | ✅ version 字段併發控制 |
| SQL 注入防護 | ✅ MyBatis 參數綁定 |
| 異常處理 | ✅ 統一拋出 RuntimeException |

## 📝 待辦項目

- [ ] 場地名稱優化：SQL LEFT JOIN venues 表
- [ ] 性能測試：高併發場景驗證
- [ ] 單元測試：編寫 JUnit 測試用例
- [ ] 集成測試：測試數據庫交互
- [ ] API 文檔：Swagger/OpenAPI 同步
- [ ] 前端集成：確認 API 契約契合

---

**檢查日期**：2026-04-03  
**檢查者**：GitHub Copilot  
**最終狀態**：✅ 全部完成，可進入測試階段

