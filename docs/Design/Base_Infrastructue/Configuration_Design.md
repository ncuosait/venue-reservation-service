# 配置設計文檔 (Configuration Design)

本文檔詳細說明 `config` 目錄下三個配置類別的功能與未來擴展方向。

---

## 1. SecurityConfig

### 目前功能

`SecurityConfig` 是 Spring Security 的配置類別，負責應用程式的安全防護機制。主要功能包括：

| 功能項目 | 詳細說明                                                                                        |
|---------|---------------------------------------------------------------------------------------------|
| **CSRF 防護關閉** | 由於目前在開發階段，已禁用 CSRF 防護。開發階段允許跨域請求直接通過。                                                       |
| **Swagger 路徑放行** | 允許公開訪問 Swagger UI 和 API 文件（`/swagger-ui/**`, `/v3/api-docs/**`, `/swagger-ui.html`），便於開發測試。 |
| **API 請求許可** | 所有 API 請求預設允許通過，實際的認證與授權邏輯委託給 `MockAuthInterceptor` 攔截器處理。                                  |
| **安全過濾鏈** | 透過 `SecurityFilterChain` Bean 配置 HTTP 安全策略。                                                 |

**當前架構優勢：**
- 簡化開發流程，允許快速進行 API 測試
- 靈活的認證機制，與自定義的攔截器相結合
- 清晰的職責劃分：SecurityConfig 負責基礎安全框架，MockAuthInterceptor 處理具體的認證邏輯

### 未來擴展方向

#### 2.1 正式的 JWT 認證集成
- **目標**：將 Mock 認證升級為正式的 JWT (JSON Web Token) 認證
- **實現方式**：
  - 在 SecurityConfig 中配置 JWT 過濾器，取代 MockAuthInterceptor
  - 使用 `HttpSecurity` 的 `authenticationProvider()` 方法配置自定義認證提供商
  - 實現 JWT Token 的驗證與刷新機制
- **預期效果**：提高系統安全性，實現無狀態認證

#### 2.2 細粒度權限控制 (RBAC)
- **目標**：實現基於角色的存取控制 (Role-Based Access Control)
- **實現方式**：
  - 在 `securityFilterChain` 中添加 `.hasRole()` 或 `.hasAuthority()` 條件
  - 定義不同的 URL 路徑對應不同的角色要求
  - 結合資料庫中的使用者角色表進行動態權限檢查
- **範例路徑配置**：
  ```
  /api/admin/** → 需要 ADMIN 角色
  /api/user/** → 需要 USER 或 ADMIN 角色
  /api/public/** → 允許所有人訪問
  ```

#### 2.3 OAuth2 / OpenID Connect 支持
- **目標**：支持第三方身份認證供應商（如 Google、GitHub、Microsoft）
- **實現方式**：
  - 新增 `OAuth2ResourceServerConfigurer` 配置
  - 配置信任的 OAuth2 提供商端點
  - 實現用戶信息映射與自動註冊邏輯

#### 2.4 動態安全策略加載
- **目標**：將安全配置從代碼中分離，支持運行時動態調整
- **實現方式**：
  - 將安全規則存儲在資料庫或配置文件中
  - 實現配置刷新機制，無需重啟應用即可更新安全策略
  - 支持針對不同環境（開發、測試、生產）的不同安全配置

#### 2.5 增強的日誌與審計
- **目標**：記錄所有安全事件，便於問題排查和合規審計
- **實現方式**：
  - 在 SecurityConfig 中配置 `SecurityEventListener`
  - 記錄失敗的認證嘗試、權限拒絕等事件
  - 集成審計日誌系統（如 ELK Stack）

---

## 2. SwaggerConfig

### 目前功能

`SwaggerConfig` 使用 SpringDoc OpenAPI 3.0 標準配置 API 文檔。主要功能包括：

| 功能項目 | 詳細說明 |
|---------|--------|
| **API 基本信息** | 定義 API 文檔的標題為「場地租借系統 API」，版本號為「1.0」 |
| **安全方案定義** | 在 Swagger UI 中定義名為 `Mock-Authorization` 的安全方案，告知文檔需要在 HTTP Header 中傳遞 `Authorization` 令牌 |
| **API 密鑰認證** | 配置 `APIKEY` 類型的安全認證，在 HTTP Header 中傳遞 |
| **自動文檔生成** | 基於 Controller 和 API 方法的註解自動生成互動式文檔，可在 `/swagger-ui.html` 查閱 |

**當前架構優勢：**
- 簡潔的配置，快速啟用 API 文檔
- 集成 Mock 認證方案，便於測試
- 支持在 Swagger UI 中直接測試 API（包含 Authorization Header）

### 未來擴展方向

#### 2.1 細化安全方案文檔
- **目標**：提供更詳細的 JWT Token 使用說明
- **實現方式**：
  - 更新 `SecurityScheme` 描述信息
  - 添加 Token 格式說明（如 `Bearer <token>`）
  - 提供示例 Token 和過期時間說明
- **代碼片段**：
  ```java
  new SecurityScheme()
      .name("Authorization")
      .type(SecurityScheme.Type.APIKEY)
      .in(SecurityScheme.In.HEADER)
      .description("傳遞 JWT Token，格式為 Bearer <token>")
      .example("Bearer eyJhbGciOiJIUzI1NiIs...")
  ```

#### 2.2 多認證方案支持
- **目標**：支持多種認證方式（JWT、OAuth2、API Key 等）
- **實現方式**：
  - 新增多個 `SecurityScheme` 定義
  - 配置 OR/AND 邏輯組合多個安全方案
  - 為不同的 API 端點指定不同的認證要求
- **預期效果**：支持不同類型的客戶端（Web、移動、第三方集成）

#### 2.3 詳細的 API 分組與分類
- **目標**：將 API 按功能模塊分組，提高文檔易讀性
- **實現方式**：
  - 定義多個 `GroupedOpenApi` Bean，按模塊劃分 API
  - 為每個分組設置獨立的描述與聯絡信息
  - 使用 `@Tag` 註解為 Controller 分類
- **分組示例**：
  - 場地管理 API (Venue Management)
  - 預約管理 API (Booking Management)
  - 用戶管理 API (User Management)
  - 系統設置 API (System Configuration)

#### 2.4 請求/回應示例補充
- **目標**：在文檔中提供真實的請求/回應示例
- **實現方式**：
  - 在 DTO 類別上使用 `@Schema` 註解添加描述
  - 使用 `@io.swagger.v3.oas.annotations.media.ExampleObject` 提供具體示例
  - 為常見的錯誤情況（4xx、5xx）提供範例回應
- **範例**：
  ```java
  @Schema(example = "{\"bookingId\": \"B001\", \"status\": \"confirmed\"}")
  private String responseExample;
  ```

#### 2.5 API 版本管理
- **目標**：支持多個 API 版本並排，便於向後兼容
- **實現方式**：
  - 配置版本化的 API 路由（如 `/api/v1/`, `/api/v2/`）
  - 為每個版本創建獨立的 `GroupedOpenApi` 配置
  - 在文檔中清晰標記棄用（Deprecated）的端點
- **預期效果**：支持平滑的 API 版本升級和遷移

#### 2.6 外部 API 文檔鏈接
- **目標**：集成第三方 API 文檔和外部資源
- **實現方式**：
  - 在 Info 中添加聯絡信息、支持郵箱、許可證等
  - 提供指向相關文檔的外部鏈接（如資料庫設計、系統架構設計）
  - 支持在 Swagger UI 中集成外部 OpenAPI 規範

---

## 3. WebConfig

### 目前功能

`WebConfig` 是 Spring Web MVC 的全局配置類別，負責應用層的請求處理與序列化配置。主要功能包括：

| 功能項目 | 詳細說明 |
|---------|--------|
| **攔截器註冊** | 註冊 `MockAuthInterceptor` 攔截器，對所有 `/api/**` 路徑的請求進行攔截 |
| **路徑排除配置** | 排除 `/api/public/**` 路徑，使公開 API 無需認證 |
| **ObjectMapper Bean** | 配置 Jackson ObjectMapper，用於 JSON 的序列化與反序列化，確保全應用使用統一的 JSON 處理策略 |

**當前架構優勢：**
- 集中管理 Web 層配置，便於維護
- 靈活的攔截器機制，支持在請求前後進行業務邏輯處理
- 統一的 JSON 處理配置，保證數據序列化的一致性

### 未來擴展方向

#### 3.1 CORS (跨域資源共享) 配置
- **目標**：允許前端應用從不同來源訪問 API
- **實現方式**：
  - 實現 `addCorsMappings()` 方法
  - 配置允許的來源、HTTP 方法、請求頭等
  - 支持 Preflight 請求處理
- **代碼示例**：
  ```java
  @Override
  public void addCorsMappings(CorsRegistry registry) {
      registry.addMapping("/api/**")
              .allowedOrigins("https://app.example.com")
              .allowedMethods("GET", "POST", "PUT", "DELETE")
              .allowCredentials(true);
  }
  ```

#### 3.2 多個攔截器鏈
- **目標**：支持多個攔截器協同工作，處理不同的業務邏輯
- **實現方式**：
  - 創建專用攔截器類別：
    - `LoggingInterceptor` - 記錄請求/回應日誌
    - `PerformanceInterceptor` - 統計 API 性能
    - `ValidationInterceptor` - 統一的輸入驗證
  - 在 WebConfig 中有序註冊多個攔截器
  - 為每個攔截器配置不同的路徑匹配規則
- **預期效果**：提高代碼組織性，降低攔截器複雜度

#### 3.3 全局異常處理整合
- **目標**：將 GlobalExceptionHandler 與 WebConfig 更緊密地整合
- **實現方式**：
  - 在 WebConfig 中註冊 `HandlerExceptionResolver`
  - 統一處理攔截器中拋出的異常
  - 返回標準化的錯誤響應格式
- **預期效果**：提高異常處理的一致性和可追溯性

#### 3.4 高級 ObjectMapper 配置
- **目標**：增強 JSON 序列化的靈活性和安全性
- **實現方式**：
  - 配置日期時間格式（使用 `@JsonFormat` 或全局 DateFormat）
  - 啟用 SerializationFeature：忽略未知字段、排除 null 值等
  - 配置自定義序列化器/反序列化器
  - 支持 JSON 視圖（JsonView）進行數據分層展示
- **代碼示例**：
  ```java
  ObjectMapper mapper = new ObjectMapper();
  mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  mapper.registerModule(new JavaTimeModule());
  ```

#### 3.5 內容協商 (Content Negotiation)
- **目標**：支持多種數據格式（JSON、XML、CSV 等）
- **實現方式**：
  - 在 WebConfig 中配置 `configureContentNegotiation()`
  - 根據 Accept Header 自動選擇合適的數據格式
  - 為不同格式配置對應的視圖解析器
- **預期效果**：提供更靈活的客戶端適配能力

#### 3.6 靜態資源與資源版本化
- **目標**：優化靜態資源加載與緩存策略
- **實現方式**：
  - 配置 `addResourceHandlers()` 添加靜態資源映射
  - 實現資源版本化（Resource Versioning），支持文件內容哈希或版本號
  - 配置長期緩存策略，提高網絡性能
- **代碼示例**：
  ```java
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
      registry.addResourceHandler("/static/**")
              .addResourceLocations("classpath:/static/")
              .setCachePeriod(31536000); // 1 年
  }
  ```

#### 3.7 性能監控與指標收集
- **目標**：在 Web 層實現性能監控
- **實現方式**：
  - 創建 `PerformanceInterceptor` 統計請求處理時間
  - 集成 Micrometer 或其他監控框架
  - 在 WebConfig 中註冊監控相關的配置
  - 收集 API 調用頻率、慢查詢等指標
- **預期效果**：便於性能瓶頸分析和系統優化

---

## 配置檔案間的協作關係

```
┌─────────────────────────────────────────────────────────────┐
│                     HTTP 請求流程                           │
└─────────────────────────────────────────────────────────────┘
                           │
                           ▼
         ┌──────────────────────────────────────┐
         │   SecurityConfig 安全過濾鏈          │
         │   (CSRF 檢查、路徑授權)              │
         └──────────────────────────────────────┘
                           │
                           ▼
         ┌──────────────────────────────────────┐
         │   WebConfig 攔截器                   │
         │   (MockAuthInterceptor - 認證檢查)    │
         └──────────────────────────────────────┘
                           │
                           ▼
         ┌──────────────────────────────────────┐
         │   Controller 處理請求                │
         └──────────────────────────────────────┘
                           │
                           ▼
         ┌──────────────────────────────────────┐
         │   ObjectMapper 序列化回應            │
         │   (WebConfig 配置)                   │
         └──────────────────────────────────────┘
                           │
                           ▼
         ┌──────────────────────────────────────┐
         │   SwaggerConfig 文檔生成              │
         │   (API 文檔在 /swagger-ui 展示)      │
         └──────────────────────────────────────┘
```

---

## 總結與建議

### 當前狀態
- ✅ 基礎配置完整，支持開發階段的 API 測試
- ✅ MockAuthInterceptor 提供簡單的認證機制
- ✅ Swagger 文檔自動生成，便於開發者參考

### 短期優先級 (Next Sprint)
1. **增強 ObjectMapper 配置** - 添加日期格式、忽略未知字段等
2. **CORS 支持** - 如果有前端應用需要跨域訪問
3. **多攔截器實現** - 添加日誌和性能監控

### 中期優先級 (1-2 個月)
1. **正式 JWT 認證** - 替換 Mock 認證
2. **RBAC 權限控制** - 實現細粒度的角色權限
3. **詳細 API 分組** - 改進 Swagger 文檔結構

### 長期優先級 (3-6 個月)
1. **OAuth2 集成** - 支持第三方認證
2. **API 版本管理** - 支持多版本共存
3. **動態安全策略** - 實現配置化的安全規則


