---
name: module-dev-standard
description: 定義開發新模組或修改內容時的標準工作流，確保代碼結構與設計文件嚴格一致。
---

# 共同原則
- **設計先行**：開始任何編碼前，必須讀取對應模組的 `XXX_API_Design.md`、`XXX_Service_Design.md` 與資料庫表設計。
- **上下文一致性**：開發時需遵循 `docs/Design/Base_Infrastructue` 中的 `Result.java` 與 `UserContext.java` 規範。
- **風格約束**：強制遵守 `.github/skills/enforcing-code-standard/SKILL.md` 中的 Java 命名慣例與「禁用 Stream/Lambda」規則。

# 開發順序 & 參考文件

## Step 1: Entity (數據實體)
- **職責**：建立與資料庫表 1:1 對應的 POJO 物件。
- **參考**：`docs/Design/DB_Design/function_tables.md`
- **位置**：`src/main/java/tw/edu/ncu/osa/venue_reservation_service/model/entity`

## Step 2: Mapper (數據持久層)
- **職責**：定義 MyBatis 介面與 XML 映射邏輯。
- **參考**：`docs/Design/Mapper_Design/XXX_Mapper_Design.md`
- **位置**：
    - Java: `src/main/java/tw/edu/ncu/osa/venue_reservation_service/mapper`
    - XML: `src/main/resources/mapper`

## Step 3: Service (業務邏輯層)
- **職責**：實作核心業務邏輯與事務控管（如時段衝突檢查）。
- **參考**：`docs/Design/Service_Design/XXX_Service_Design.md`
- **位置**：
    - 介面: `src/main/java/tw/edu/ncu/osa/venue_reservation_service/service`
    - 實作: `src/main/java/tw/edu/ncu/osa/venue_reservation_service/service/impl`

## Step 4: Controller & DTO/VO (接口層)
- **職責**：曝露 RESTful API 並定義輸入 (DTO) 與輸出 (VO) 結構。
- **參考**：`docs/Design/API_Design/XXX_API_Design.md`
- **位置**：
    - Controller: `src/main/java/tw/edu/ncu/osa/venue_reservation_service/controller`
    - DTO/VO: `src/main/java/tw/edu/ncu/osa/venue_reservation_service/model/dto` (或 `/vo`)

# 輔助模組說明
開發時必須先行理解 `docs/Design/Base_Infrastructue` 中的基礎設施：
- **異常處理**：使用 `GlobalExceptionHandler` 統一回傳格式。
- **統一響應**：所有 API 必須回傳 `Result<T>`。

# Code Review 檢查清單
LLM Agent 在完成編碼後應自我審查：
1. **併發安全**：更新操作是否包含 `version` 欄位以實作樂觀鎖？
2. **時段運算**：時段判斷是否使用了 Bitmasking 位元運算 (`&`)？
3. **代碼風控**：是否含有 Lambda 表達式或 Stream API？（嚴禁使用）。
4. **註解合規**：所有邏輯區塊與段落分隔符（`// ===`）是否使用繁體中文？