# 預約核心引擎：模組 Service 設計文件 (V1.0)

**日期：** 2026-04-03  
**類別：** `tw.edu.ncu.osa.venue_reservation_service.service.BookingService`

## 一、 設計概述

`Service` 層遵循 **單一職責原則 (SRP)**，僅處理與預約相關的業務邏輯。為了提升系統的健壯性，我們將利用 Spring 的聲明式事務（`@Transactional`）確保多表操作的原子性，並透過拋出特定的 `RuntimeException` 配合 `GlobalExceptionHandler` 來達成統一的錯誤響應格式。

## 二、 核心方法說明

### 1. 提交預約申請 (`createBooking`)

* **功能描述**：驗證時段是否衝突，並建立預約案及其關聯設備紀錄。
* **參數**：`BookingRequestDTO`（包含場地、日期、時段及聯絡資訊）。
* **回傳**：`Long` (新建立的預約案 ID)。
* **詳細邏輯流程**：
    1.  **用戶上下文提取**：從 `UserContext` 獲取當前請求者的 `userId`。
    2.  **時段遮罩轉換**：調用工具類將前端傳入的 `List<Integer>` 轉換為 24-bit 的整數遮罩。
    3.  **衝突判定機制**：調用 `BookingMapper` 查詢資料庫中同場地、同日期是否存在狀態為「已通過 (2)」且位元運算結果不為 0 的紀錄。
    4.  **物件映射與轉換**：
        * 建立 `Booking` 實體物件。
        * 使用 `ObjectMapper` 將聯絡資訊物件（`ContactInfoDTO`）序列化為 JSON 字串存儲。
    5.  **持久化操作**：執行 `insertBooking` 寫入主表，並利用其產生的自增 ID 寫入 `booking_equipment` 關聯表。
* **異常處理**：
    * 若衝突檢查發現重疊，拋出 `RuntimeException("該時段已被其他已通過之申請佔用")`。
    * 若 JSON 轉換失敗，拋出 `RuntimeException("資料格式錯誤")` 並記錄 Error Log。

### 2. 獲取個人申請列表 (`getMyBookings`)

* **功能描述**：查詢當前登入者所提交的所有預約案。
* **邏輯說明**：
    1.  從 `UserContext` 獲取 `userId`。
    2.  調用 Mapper 查詢該用戶所有紀錄。
    3.  將 `time_slots` 位元遮罩逆向轉換為可讀的時段列表。

### 3. 撤回預約申請 (`withdrawBooking`)

* **功能描述**：使用者主動取消尚未執行的預約案。
* **邏輯說明**：
    1.  檢查該預約案是否存在且屬於當前登入者。
    2.  檢查狀態：僅限狀態為「審核中」或「已通過」時可撤回。
    3.  更新狀態為 `WITHDRAWN (0)` 並遞增版本號（樂觀鎖）。
* **異常處理**：
    * 若狀態不符（如已被拒絕），拋出 `RuntimeException` 說明原因。

## 三、 異常處理與事務策略

### 1. 事務管理 (Transaction)
* 所有涉及寫入（Insert, Update, Delete）的方法必須標註 `@Transactional(rollbackFor = Exception.class)`。
* **復原範圍**：當 `insertBookingEquipment` 失敗或發生業務異常時，會自動回滾已寫入的 `bookings` 主表紀錄，確保資料完整。

### 2. 與全局攔截器的協作機制
本模組不主動捕獲異常並封裝 `Result`，而是直接拋出異常交由 `GlobalExceptionHandler` 處理：
* **業務異常**：拋出 `RuntimeException`。攔截器會捕獲它，記錄 Log 後回傳 `Result.error(e.getMessage())`。
* **校驗異常**：當 `DTO` 違反註解（如 `@NotNull`）時，系統自動拋出 `MethodArgumentNotValidException`。攔截器會提取預設錯誤訊息並回傳 `Result.error(msg)`。
* **非預期異常**：如資料庫連線中斷（`Exception`），攔截器會回傳「系統繁忙，請聯絡管理員」以維護安全性。

## 四、 效能優化與擴展建議

1.  **樂觀鎖應用**：在 `createBooking` 初始化時 `version` 設為 1，後續所有更新操作必須帶上版本號進行比對，防止高併發下的 Race Condition。
2.  **延遲序列化**：聯絡資訊使用 JSON 存儲能提供良好的擴展性（例如未來增加 Line ID 等欄位），無需修改資料庫 Schema。
3.  **大對象處理**：若 `equipmentIds` 列表過大，考慮改為批次寫入（Batch Insert）以提升效能。

