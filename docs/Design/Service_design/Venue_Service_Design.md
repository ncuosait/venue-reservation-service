# 場地與組織模組：模組 Service 設計文件 (V1.0)

**日期：** 2026-04-04  
**類別：** `tw.edu.ncu.osa.venue_reservation_service.service.VenueService`

## 一、 設計概述

`VenueService` 負責管理系統中的靜態元數據（Metadata），包含組織單位與場地資訊。由於場地資訊涉及多個資料表（`venues`、`equipments`、`venue_equipment_map`）的關聯，本層級的核心任務在於數據的整合與封裝。此外，考量到本模組屬於 **Read-Heavy（高頻讀取）** 性質，設計上優先優化查詢效率，並預留未來快取（Cache）擴展空間。

## 二、 核心方法說明

### 1. 獲取所有管理單位 (`getAllUnits`)

* **功能描述**：檢索系統中所有已啟用的行政單位清單。
* **回傳**：`List<UnitVO>`。
* **邏輯說明**：
    1.  調用 `UnitMapper` 查詢 `units` 表中的所有紀錄。
    2.  將 `Unit` 實體轉換為 `UnitVO` 進行回傳。
    3.  **擴展考量**：目前 MVP 階段預設回傳「學務處」資訊，未來可依據狀態欄位進行動態過濾。

### 2. 根據單位查詢場地清單 (`getVenuesByUnitId`)

* **功能描述**：提供特定單位下屬的所有場地資訊，並包含該場地可借用的設備列表。
* **參數**：`unitId` (Long)。
* **回傳**：`List<VenueVO>`。
* **詳細邏輯流程**：
    1.  **資料檢索**：調用 `VenueMapper` 根據 `unitId` 查詢場地基本資訊。
    2.  **數據組裝 (Data Assembly)**：
        * 對於查詢到的每個場地，調用 `EquipmentMapper` 查詢其在 `venue_equipment_map` 中關聯的設備紀錄。
        * 將設備列表封裝進 `VenueVO` 的 `equipments` 欄位中。
    3.  **防禦性編程**：若該單位下無場地，應回傳空列表而非拋出異常。

### 3. 獲取場地詳細資訊 (`getVenueById`)

* **功能描述**：取得單一場地的完整資訊與規則說明，供預約表單渲染使用。
* **參數**：`id` (Long)。
* **回傳**：`VenueVO`。
* **邏輯說明**：
    1.  **主體查詢**：從 `venues` 表中獲取基本欄位（如容量、描述）。
    2.  **設備關聯**：一次性抓取該場地所屬的所有設備（`equipments`）。
    3.  **封裝回傳**：若場地不存在，拋出 `RuntimeException("找不到該場地資訊")`。

## 三、 異常處理與事務策略

### 1. 事務管理 (Transaction)
* **唯讀優化**：本模組的方法主要為查詢操作，建議標註 `@Transactional(readOnly = true)` 以優化資料庫效能。
* **一致性保證**：雖然不涉及複雜的寫入，但若未來加入場地資訊修改功能，必須確保主表與關聯表（`venue_equipment_map`）的修改在同一個事務中完成。

### 2. 全局異常攔截協作
* **資訊隱藏**：當查詢到不存在的 ID 時，Service 層拋出具備業務語意的異常，由 `GlobalExceptionHandler` 捕獲並轉化為統一的 `Result.error()` 響應。
* **參數校驗**：依賴 Spring Validation 處理 DTO 的基礎格式檢查，Service 層專注於業務邏輯校驗（例如：單位 ID 是否有效）。

## 四、 效能優化與擴展建議

1.  **快取機制 (Future Work)**：場地與單位資料變動頻率極低，適合將 `getAllUnits` 與 `getVenueById` 的結果存儲於 Redis 中，大幅提升響應速度。
2.  **N+1 查詢問題優化**：在獲取場地列表時，應避免對每個場地單獨發起 SQL 查詢設備。建議使用 MyBatis 的 `collection` 標籤實作 **左連接 (Left Join)** 或 **巢狀查詢**，在一次請求中完成資料組裝。
3.  **多單位橫向擴展**：透過 `unit_id` 的索引設計，系統可支援未來多個行政單位（如住服組）同時在線，實現多租戶隔離。
