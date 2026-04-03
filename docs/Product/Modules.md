## 功能模組拆解文件 (Module Decomposition)

為了符合軟體工程的**高內聚、低耦合**原則，將系統拆分為 5 大模組。能讓開發時職責分明，且有利於單元測試。

### 1. 核心驗證模組 (Auth & User Context)

- **職責：** 處理身分識別與權限判斷。
- **關鍵組件：**
  - `MockAuthInterceptor`: 負責識別 Mock Token 並解析身分。
  - `UserContext`: 提供全局存取當前登入者資訊的工具。
- **軟工思維：** 基礎設施層，為後續業務模組提供「是誰」的依據。

### 2. 場地與組織模組 (Venue & Unit Metadata)

- **職責：** 維護系統的靜態資料（單位、場地資訊）。
- **關鍵組件：**
  - `UnitService`: 取得單位列表（學務處、住服組等）。
  - `VenueService`: 提供場地詳情、容納人數限制查詢。
- **軟工思維：** **Read-Heavy** 模組，未來可考慮加入快取（Cache）優化。

### 3. 預約核心引擎 (Booking Engine)

- **職責：** 處理最複雜的時段運算與狀態流轉。
- **關鍵組件：**
  - `BitmaskCalculator`: 負責「時間範圍 ↔ 位元整數」的轉換。
  - `ConflictChecker`: 實作 `(existing & new) != 0` 的邏輯。
  - `BookingService`: 處理建立申請、衝突檢查、狀態更新（Pending -> Approved）。
- **軟工思維：** **Domain Logic** 核心。所有的業務規則（如：一天只能申請一次）都封裝在此。

### 4. 審核與管理模組 (Admin & Audit)

- **職責：** 管理員視角的業務操作與行為存證。
- **關鍵組件：**
  - `ApprovalService`: 管理員通過/拒絕操作。
  - `AuditLogAspect`: 使用 **AOP (Aspect-Oriented Programming)** 自動攔截管理員行為並寫入日誌。
- **軟工思維：** 確保系統的**可追蹤性 (Observability)**。

### 5. 通用服務模組 (Common & Integration)

- **職責：** 提供全局支撐的功能。
- **關鍵組件：**
  - `EmailService`: 串接 `spring-boot-starter-mail` 發送審核結果通知。
  - `GlobalExceptionHandler`: 攔截所有報錯，轉化為 `Result.error()`。
- **軟工思維：** **Cross-cutting Concerns**，處理與業務無關但系統必備的共通功能。

---

### 📂 模組開發優先順序建議：

1.  **模組 1 & 5** (基礎架構)：確保 API 跑得通、不會崩潰、有假登入。
2.  **模組 2 & 3** (核心業務)：實作場地查詢與預約申請，包含 Bitmasking 邏輯。
3.  **模組 4** (管理功能)：最後加入審核與 Log 紀錄。
