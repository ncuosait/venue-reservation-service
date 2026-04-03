---
name: enforcing-code-standard
description: 定義 Java Spring Boot 團隊的程式碼撰寫規範。每次與 AI 協作撰寫程式碼（如 Controller, Service, Mapper）或進行 Code Review 時，都應要求 AI 遵循此文件。主要包含繁體中文註解、Java 命名慣例、禁用 Stream/Lambda 及 Spring Boot 架構原則。
---

# 程式碼開發規範 (Java Spring Boot 版)

本文件定義團隊開發的基準，旨在提升 Java 代碼的可讀性、可維護性及系統穩定性。

## 1. 命名慣例 (Naming Conventions)

為了符合 Java 生態系的標準慣例，命名規則如下：

- **變數與方法**：必須使用 `camelCase` (小駝峰式命名)。
  - **好的例子**：`userProfile`, `getStudentData`, `isAdminAuthenticated`。
  - **壞的例子**：`user_profile`, `UserData`, `a1`。
- **類別與介面**：必須使用 `PascalCase` (大駝峰式命名)。
  - **範例**：`BookingService`, `VenueMapper`, `BookingRequestDTO`。
- **具備意圖**：命名必須能反映用途，避免使用模糊縮寫（如 `req` 應寫為 `request`）。
- **常數**：必須全大寫並以底線分隔，例如 `MAX_RETRY_COUNT = 3`。

## 2. 語法與可讀性 (Syntax & Readability)

為了確保邏輯直覺且易於追蹤，優先選擇傳統語法：

- **禁用進階匿名語法**：嚴格禁止使用 Java 8+ 的 `Lambda` 表達式與 `Method Reference`。
- **使用傳統迴圈**：處理集合資料時，請使用傳統 `for`、增強型 `for-each` 或 `while` 迴圈，**嚴禁使用 Stream API** (如 `.stream().map().filter()`)，以提升除錯（Debug）與追蹤邏輯的直覺性。
- **單一職責 (SRP)**：一個方法只做一件事。若方法超過 30 行，應考慮進行拆分。

## 3. 註解與文件 (Comments & Documentation)

- **語言要求**：所有的邏輯區塊、方法說明與變數註解必須使用 **繁體中文**。
- **註解格式**：
  - **方法註解**：每個方法必須有 **Javadoc**，說明其功能、參數 (`@param`) 與回傳值 (`@return`)。
  - **行內註解**：對於複雜邏輯，必須加上行內註解說明。
  - **類別註解**：每個類別開頭應說明其主要功能與用途。
  - **段落註解**：在程式碼中使用 `// ==========================================` 作為段落分隔符，並在上方加上標題。

  ```java
  // 範例：Service 層開發
  /**
   * 使用者服務實作類
   */
  @Service
  public class UserServiceImpl implements UserService {

      // ==========================================
      // 1. 認證與登入 (Authentication)
      // ==========================================

      /**
       * 使用者登入：驗證帳密並簽發 Token
       * @param request 包含帳號密碼的 DTO
       * @return 登入結果與 Token
       */
      @Override
      public Result<String> login(LoginRequestDTO request) {
          // 邏輯實作...
      }
  }
  ```

- **第三方套件註解**：調用如 `MyBatis`, `Spring Security`, `Lombok` 時，應註解說明其預期行為。

## 4. 穩定性與架構原則 (Stability & Architecture)

參考 Clean Code 與 SOLID 原則，提升代碼強健性：

- **錯誤處理 (Error Handling)**：
  - **嚴禁空的 `catch {}`**：必須捕獲特定的異常，並透過 `log.error` 記錄具備追蹤價值的錯誤訊息。
  - **優先處理邊界情況**：使用 Guard Clauses 提早回傳，避免深層巢狀 `if-else`。
- **避免魔術數字 (No Magic Numbers)**：
  - 狀態碼、類型識別等具特殊意義的數值，必須定義為 `Enum` (列舉) 或 `static final` 常數。
- **依賴注入思維 (Dependency Injection)**：
  - 優先使用 **構造函數注入 (Constructor Injection)** 搭配 Lombok 的 `@RequiredArgsConstructor`，而非在方法內直接實例化物件。
- **資料隔離與封裝**：
  - 嚴格區分 **Entity**、**DTO** 與 **VO**。
  - 所有的 API 回傳必須封裝在統一的 `Result<T>` 類別中。

## 5. 資料庫操作 (Persistence Layer)

- **Mapper 規範**：
  - 複雜 SQL 應寫在 `Mapper.xml` 中。
  - 衝突檢查應優先利用資料庫運算（如位元運算 `&`）而非在 Java 層過濾。
  - 更新操作必須考慮 **樂觀鎖 (Optimistic Locking)**，使用 `version` 欄位確保併發安全。