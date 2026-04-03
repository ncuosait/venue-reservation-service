# 後端專案目錄架構設計文件 (Project Structure Design)

**日期：** 2026-03-18  
**版本號：** V1.0 (Initial Architecture)  
**撰寫人：** Senior Tech Leader (Mentor)

---

## 一、 整體設計思想

本專案採用 **分層架構 (Layered Architecture)**，並結合 **物件導向設計原則 (SOLID)**。核心設計思想包含：

### 1. 關注點分離 (Separation of Concerns, SoC)
每一層只負責一件特定的事情。例如，`Controller` 只負責協調請求，不應包含業務邏輯；`Service` 只負責業務邏輯，不應知道資料庫是 MySQL 還是 MongoDB。

### 2. 資料封裝與層級隔離 (Data Encapsulation & Isolation)
嚴格區分 **Entity、DTO 與 VO**。
* **Entity** 是資料庫的影子，不應直接曝露給前端。
* **DTO** 負責承接輸入，保護內部模型不被外部隨意修改。
* **VO** 負責輸出，確保只回傳前端需要的欄位，避免資安風險（如不小心回傳用戶密碼）。

### 3. 面向介面開發 (Interface-based Programming)
`Service` 層採用介面與實現分離 (`Service` & `ServiceImpl`)。
* **軟工思維：** 這是為了實現 **依賴反轉原則 (Dependency Inversion Principle)**。這讓單元測試更容易進行（透過 Mock 介面），也讓未來若要更換業務邏輯實現時，不需要修改調用方的代碼。

---

## 二、 目錄詳細架構說明

| 資料夾名稱 | 功能描述 | 內含組件與設計重點 |
| :--- | :--- | :--- |
| **`config`** | 系統環境配置 | 包含 MyBatis 配置、CORS 跨域配置、以及你之後會補上的 WebMVC 攔截器配置。 |
| **`controller`** | API 進入點 | 負責定義 Endpoint、處理 HTTP Method。重點在於使用 `@Valid` 進行基礎參數校驗。 |
| **`service`** | 業務邏輯層 | **核心大腦**。處理如「預約時段是否衝突」、「計算預約人數」等業務判斷。 |
| **`mapper`** | 數據持久層 | MyBatis 的介面定義，與 XML 檔案或註解 SQL 配合，負責與 DB 通訊。 |
| **`model`** | 數據模型 | 拆分為 `entity` (DB對應)、`dto` (輸入包)、`vo` (輸出包) 三個子目錄。 |
| **`security`** | 安全與認證 | 存放 OAuth2 Client 的回調處理邏輯與 Spring Security 的權限控管設定。 |
| **`interceptor`** | 全局攔截器 | 實現 `HandlerInterceptor`。用於 MVP 階段的 Mock 登入與全局請求日誌追蹤。 |
| **`exception`** | 異常處理中心 | 存放 `@RestControllerAdvice`。將所有報錯轉化為標準 JSON 格式回傳給前端。 |
| **`common`** | 通用公用件 | 存放 `Result<T>` 統一回傳格式與全專案通用的 `Constant` (如狀態碼定義)。 |
| **`util`** | 工具類 | 與業務邏輯無關的純函式，例如 `Bitmasking` 運算工具、日期轉換工具。 |

---

## 三、 軟體工程思維深度解析

### 1. 為什麼要「統一回傳格式 (`common.result`)」？
* **工程思維：** **契約式開發 (Design by Contract)**。
* 前端不需要猜測後端這次會回傳什麼。無論成功或失敗，前端收到的結構永遠是 `{ "success": boolean, "data": T, "message": string }`。這能極大簡化前端的 Axios 攔截器邏輯，提升協作效率。

### 2. 為什麼要「全局異常處理 (`exception`)」？
* **工程思維：** **Fail-fast 與系統健壯性**。
* 在傳統開發中，沒處理好的 Exception 會導致後端直接噴出 Stack Trace 到瀏覽器，這不僅對用戶不友善，更是資安漏洞。透過全局攔截，我們能優雅地「捕獲」錯誤並回傳可理解的訊息。

### 3. 為什麼要「分層 DTO/VO」？
* **工程思維：** **最小權限原則 (Least Privilege)**。
* 假設 `UserEntity` 有個欄位是 `password`。如果我們直接把 Entity 丟給前端，即便前端沒顯示，駭客也能從 API Response 看到。分層確保了我們「只給出該給的」，並「只接收該收的」。

---

## 四、 若不依據此設計可能產生的問題

1.  **大雜燴 (Big Ball of Mud)：** 如果把邏輯全寫在 `Controller`，代碼會變得極難閱讀且無法進行單元測試。面試官看到這種結構會認為你缺乏維護大型系統的能力。
2.  **資料庫耦合：** 若前端直接操作 Entity，當未來資料庫欄位名稱修改時，前端的代碼也會跟著壞掉。分層設計提供了緩衝帶 (Buffer)，讓後端內部修改不影響對外接口。
3.  **除錯困難：** 缺乏統一錯誤處理與攔截器，當線上環境出錯時，你將難以追蹤請求的輸入參數與錯誤發生的脈絡。
