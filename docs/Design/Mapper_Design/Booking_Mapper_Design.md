# 預約核心引擎：模組 Mapper 設計文件 (V1.1)

**日期：** 2026-04-03  
**類別：** `tw.edu.ncu.osa.venue_reservation_service.mapper.BookingMapper`

## 一、 設計概述

Mapper 層負責將 Java 物件的操作轉化為 SQL 指令，與資料庫進行交互。本模組使用 MyBatis 框架，核心設計重點在於利用資料庫層級的位元運算執行高效的時段衝突檢查，並透過樂觀鎖（Optimistic Locking）確保高併發環境下的數據一致性。

## 二、 Mapper 介面定義 (`BookingMapper.java`)

`BookingMapper` 介面定義了預約數據完整生命週期所需的持久化操作：

| 方法名稱 | 參數 | 回傳值 | 功能描述 |
| :--- | :--- | :--- | :--- |
| `countConflictingApprovedBookings` | `venueId`, `date`, `mask` | `int` | 檢查特定日期與場地中，是否存在與給定遮罩重疊且狀態為「已通過」的預約。 |
| `insertBooking` | `Booking` 實體 | `int` | 向 `bookings` 表插入新預約，並自動獲取產生的主鍵 ID。 |
| `insertBookingEquipment` | `bookingId`, `equipmentId` | `int` | 建立預約案與借用設備之間的關聯紀錄。 |
| `selectByUserId` | `userId` | `List<Booking>` | 根據用戶 ID 檢索其所有的歷史預約申請紀錄。 |
| `updateStatusWithVersion` | `id`, `newStatus`, `oldVersion` | `int` | 執行狀態變更（如審核或撤回），並透過版本號驗證實作樂觀鎖。 |

## 三、 SQL 映射規範 (`BookingMapper.xml`)

XML 配置檔案定義了具體的 SQL 邏輯，包含核心預約流程與後續擴展的維護邏輯：

### 1. 核心衝突檢查邏輯
```xml
<select id="countConflictingApprovedBookings" resultType="int">
    SELECT COUNT(*) FROM bookings
    WHERE venue_id = #{venueId}
    AND booking_date = #{date}
    AND status = 2
    AND (time_slots &amp; #{mask}) != 0
</select>
```
* **技術說明**：此 SQL 僅過濾 `status = 2` (APPROVED) 的申請案。
* **運算原理**：使用位元與運算子 `&` 判定 `time_slots` 與請求遮罩是否有重疊位元。若結果不為 0，則代表時段已被佔用。

### 2. 預約案插入與主鍵回填
```xml
<insert id="insertBooking" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO bookings (venue_id, user_id, booking_date, time_slots, status, purpose, p_count, contact_info, version)
    VALUES (#{venueId}, #{userId}, #{bookingDate}, #{timeSlots}, #{status}, #{purpose}, #{pCount}, #{contactInfo}, 1)
</insert>
```
* **自動產增 ID**：配置 `useGeneratedKeys="true"`，確保執行後將資料庫產生的 ID 自動回填至 `Booking` 物件。
* **樂觀鎖初始化**：新紀錄插入時，`version` 欄位預設初始化為 `1`。

### 3. 設備關聯處理
```xml
<insert id="insertBookingEquipment">
    INSERT INTO booking_equipment (booking_id, equipment_id)
    VALUES (#{bookingId}, #{equipmentId})
</insert>
```
* **功能**：儲存該筆預約所選定的各項設備 ID，處理一對多關聯。

### 4. 用戶申請清單檢索 (擴展實作)
```xml
<select id="selectByUserId" resultType="tw.edu.ncu.osa.venue_reservation_service.model.entity.Booking">
    SELECT * FROM bookings 
    WHERE user_id = #{userId} 
    ORDER BY created_at DESC
</select>
```
* **功能**：支援前台「我的申請」功能，依據申請時間倒序排列。

### 5. 帶樂觀鎖的狀態更新 (擴展實作)
```xml
<update id="updateStatusWithVersion">
    UPDATE bookings 
    SET status = #{newStatus}, 
        version = version + 1 
    WHERE id = #{id} AND version = #{oldVersion}
</update>
```
* **併發控制**：透過 `WHERE version = #{oldVersion}` 確保更新時數據未被他人變更。
* **應用場景**：適用於管理員審核（PENDING → APPROVED/REJECTED）或用戶撤回（PENDING/APPROVED → WITHDRAWN）。

## 四、 數據映射與性能考量

### 1. 索引優化
* 查詢邏輯嚴格遵循 `idx_venue_date (venue_id, booking_date)` 與 `idx_user_id` 索引，確保在海量數據下仍能維持毫秒級響應。

### 2. 特殊欄位處理
* **Contact Info**：在 SQL 中映射為字串，實質對應資料庫的 `JSON` 類型欄位，由 Service 層進行 JSON 序列化處理。
* **Time Slots**：資料庫採用 `INT UNSIGNED` 儲存 24-bit 遮罩，MyBatis 映射為 Java `int` 類型進行運算。