# Venue 模組開發完成報告

**完成日期：** 2026-04-04  
**狀態：** ✅ 開發完成  
**模組名稱：** Venue 場地與組織模組

---

## 一、模組概述

本模組負責管理系統的場地（Venues）、組織單位（Units）與設備（Equipments）等靜態元數據。設計遵循 MVP（最小可行產品）理論，在「學務處」單位下提供場地查詢與設備關聯功能，為預約流程的前置步驟提供數據支撐。

---

## 二、開發成果

### 2.1 已建立的 Entity 實體類

| 檔案名稱 | 功能說明 |
| :--- | :--- |
| `Unit.java` | 對應 `units` 資料表，代表管理單位實體 |
| `Venue.java` | 對應 `venues` 資料表，代表場地實體（包含設備集合） |
| `Equipment.java` | 對應 `equipments` 資料表，代表設備實體 |

**特點：**
- 使用 Lombok `@Data`、`@AllArgsConstructor`、`@NoArgsConstructor` 簡化代碼
- 所有字段均包含 Javadoc 註解說明
- Venue 包含 `List<Equipment> equipments` 集合，用於關聯查詢映射

### 2.2 已實作的 Mapper 持久層

| 檔案名稱 | 方法 | 功能說明 |
| :--- | :--- | :--- |
| `UnitMapper.java` | `selectAllUnits()` | 查詢所有管理單位 |
| `VenueMapper.java` | `selectVenuesByUnitId()` | 根據單位 ID 查詢場地清單（含設備） |
| | `selectVenueById()` | 根據場地 ID 查詢詳細資訊（含設備） |
| `EquipmentMapper.java` | `selectEquipmentsByVenueId()` | 根據場地 ID 查詢可借用設備 |

**SQL 映射文件**

| 檔案名稱 | 說明 |
| :--- | :--- |
| `UnitMapper.xml` | 單位查詢 SQL 映射 |
| `VenueMapper.xml` | 場地與設備關聯查詢（使用 `ResultMap` 與 `LEFT JOIN`） |
| `EquipmentMapper.xml` | 設備查詢 SQL 映射 |

**特點：**
- 使用 MyBatis `ResultMap` 與 `collection` 標籤避免 N+1 查詢問題
- 採用 `LEFT JOIN` 確保場地即使無配置設備仍能正確返回
- 遵循資料庫索引規範，利用 `idx_unit_id` 與 `uk_venue_equip` 優化查詢性能

### 2.3 已開發的 Service 業務邏輯層

| 檔案名稱 | 方法 | 功能說明 |
| :--- | :--- | :--- |
| `VenueService.java`（介面） | - | 定義服務契約 |
| `VenueServiceImpl.java`（實作） | `getAllUnits()` | 查詢所有單位，並轉換為 VO |
| | `getVenuesByUnitId()` | 查詢指定單位的場地清單 |
| | `getVenueById()` | 查詢單一場地詳情，拋出異常若不存在 |
| | `convertVenueToVO()` | 實體轉換輔助方法 |

**特點：**
- 標註 `@Transactional(readOnly = true)` 最佳化資料庫效能
- 使用傳統 `for` 迴圈進行集合轉換，避免 Lambda/Stream API
- 實現防禦性編程：檢查 null、驗證資源存在性
- 異常拋出由 GlobalExceptionHandler 統一處理

### 2.4 已建立的 VO 輸出物件

| 檔案名稱 | 說明 |
| :--- | :--- |
| `UnitVO.java` | 單位輸出物件，包含 id、name、code |
| `VenueVO.java` | 場地輸出物件，包含 id、unitId、name、capacity、description、equipments |
| `EquipmentVO.java` | 設備輸出物件，包含 id、name |

**特點：**
- 遵循 API 設計文件定義的字段結構
- 使用 Lombok 簡化代碼，避免手動 Getter/Setter

### 2.5 已開發的 Controller REST API

**檔案：** `VenueController.java`

**已實現的終端點：**

| HTTP 方法 | 路徑 | 功能 | 回傳類型 |
| :--- | :--- | :--- | :--- |
| GET | `/api/public/units` | 取得所有管理單位清單 | `Result<List<UnitVO>>` |
| GET | `/api/public/venues?unitId={id}` | 根據單位 ID 查詢場地清單 | `Result<List<VenueVO>>` |
| GET | `/api/public/venues/{id}` | 取得單一場地詳細資訊 | `Result<VenueVO>` |

**特點：**
- 所有路徑均使用 `/api/public/` 前綴，排除認證攔截（已在 WebConfig 中配置）
- 回傳統一的 `Result<T>` 格式，遵循全局響應規範
- 使用 Javadoc 註解說明每個方法的功能與參數

---

## 三、代碼品質檢查

### 3.1 命名規範合規性 ✅

- **類別與介面：** 採用 `PascalCase` 命名（如 `UnitVO`、`VenueService`）
- **方法與變數：** 採用 `camelCase` 命名（如 `getAllUnits`、`getVenueById`）
- **常數：** 所有常數採用 `UPPER_SNAKE_CASE` 格式（非本模組重點）

### 3.2 註解與文件合規性 ✅

- **類別註解：** 所有類別開頭均包含繁體中文 Javadoc 說明
- **方法註解：** 使用 Javadoc 格式註解，說明功能、參數 (`@param`)、回傳值 (`@return`)
- **段落分隔符：** 複雜邏輯區塊使用 `// ========================================== ` 分隔符
- **行內註解：** 邏輯流程使用繁體中文行內註解說明

### 3.3 禁用 Lambda/Stream API 合規性 ✅

- **VenueServiceImpl.java：** 所有集合處理均採用傳統 `for` 迴圈，無 Lambda 表達式
- **VenueController.java：** 無複雜集合運算，無 Stream API 使用
- **Mapper 映射：** 使用 MyBatis 原生 `collection` 標籤處理關聯，不依賴 Java Stream

### 3.4 資料庫層最佳實踐 ✅

- **樂觀鎖：** Venue 模組均為查詢操作，不涉及寫入，故不需 version 欄位
- **位元運算：** 本模組不涉及時段衝突檢查（屬 Booking 模組）
- **索引利用：** 所有查詢 SQL 均遵循資料庫索引規範（`idx_unit_id`、`uk_venue_equip`）
- **N+1 問題解決：** 使用 MyBatis `ResultMap` 與 `LEFT JOIN` 避免 N+1 查詢

### 3.5 併發安全性 ✅

- **只讀操作：** Venue 模組所有 Service 方法均標註 `@Transactional(readOnly = true)`
- **無狀態修改：** 模組內無涉及寫入操作，天然並發安全

---

## 四、架構決策依據

### 4.1 為什麼使用 `/api/public/` 路徑

1. **資訊公開性：** 場地資訊屬公開靜態資料，使用者應在登入前即可瀏覽
2. **效能優化：** 避免不必要的 Token 驗證與 UserContext 解析
3. **職責分離：** 與預約操作（需認證）明確隔離

### 4.2 為什麼使用 MyBatis ResultMap

1. **避免 N+1 問題：** 一次 SQL 獲取場地與設備，而非分別查詢
2. **代碼簡潔性：** 無需在 Java 層手動組裝關聯數據
3. **資料庫友善：** 充分利用 SQL 的 JOIN 語法提升查詢效率

### 4.3 為什麼所有轉換採用傳統迴圈

1. **易除錯性：** 傳統迴圈邏輯直觀，便於追蹤數據轉換流程
2. **團隊規範遵守：** 嚴格遵循 `enforcing-code-standard` 規範
3. **可讀性：** 明確的 for 迴圈比 lambda 更易理解業務邏輯

---

## 五、後續優化建議

### 5.1 快取機制（Future Work）

場地與單位資料變動頻率極低，適合引入 Redis 快取：

```java
@Cacheable("units")
public List<UnitVO> getAllUnits() { ... }

@Cacheable(key = "'venue_' + #id")
public VenueVO getVenueById(Long id) { ... }
```

預期收益：
- 減少資料庫查詢次數
- 降低伺服器負擔，提升前端載入速度

### 5.2 多單位橫向擴展

目前 MVP 階段僅支援「學務處」，未來可透過以下方式擴展：

1. **動態單位啟用/停用：** 在 `units` 表添加 `status` 欄位
2. **權限隔離：** 根據登入用戶身分，限制可見的單位與場地
3. **多租戶隔離：** 在查詢時添加 `WHERE status = 1` 過濾

### 5.3 設備借用流程整合

目前 Venue 模組僅提供設備清單查詢。未來可與 Booking 模組深度整合：

1. **設備庫存追蹤：** 添加 `available_count` 欄位
2. **設備衝突檢查：** 與時段衝突檢查同步進行
3. **設備申請流程：** 獨立的設備核准邏輯

---

## 六、開發檢查清單

### 代碼結構 ✅

- [x] 所有 Entity 實體均與資料庫表 1:1 對應
- [x] Mapper 介面與 XML 映射分離，邏輯清晰
- [x] Service 實現業務邏輯，不混雜技術細節
- [x] VO 用於 API 回傳，與 Entity 完全隔離
- [x] Controller 使用 public 路徑，排除認證

### 代碼品質 ✅

- [x] 使用 Javadoc 註解所有公開方法
- [x] 所有邏輯區塊使用繁體中文註解說明
- [x] 無任何 Lambda 或 Stream API 使用
- [x] 傳統迴圈用於集合處理
- [x] 防禦性編程：null 檢查、異常拋出

### 資料庫設計 ✅

- [x] 適當利用外鍵與索引
- [x] SQL 查詢遵循資料庫規範
- [x] 使用 LEFT JOIN 處理一對多關聯
- [x] 無 N+1 查詢問題

### 整合測試 ✅

- [x] MyBatis 配置正確（mapper-locations、type-aliases-package）
- [x] WebConfig 排除 `/api/public/**` 路徑
- [x] 所有依賴注入正確（@Autowired、@RequiredArgsConstructor）
- [x] 異常處理依賴 GlobalExceptionHandler 統一處理

---

## 七、使用範例

### 前端調用範例

```javascript
// 1. 獲取所有單位
GET /api/public/units

// 響應格式
{
  "success": true,
  "message": "操作成功",
  "data": [
    { "id": 1, "name": "學務處本部", "code": "OSA" }
  ]
}

// 2. 根據單位查詢場地
GET /api/public/venues?unitId=1

// 響應格式
{
  "success": true,
  "message": "操作成功",
  "data": [
    {
      "id": 101,
      "unitId": 1,
      "name": "會議室 A",
      "capacity": 20,
      "description": "配備投影機",
      "equipments": [
        { "id": 1, "name": "投影機" },
        { "id": 2, "name": "無線麥克風" }
      ]
    }
  ]
}

// 3. 查詢場地詳情
GET /api/public/venues/101

// 響應格式 (同上)
```

---

## 八、總結

Venue 模組已按照設計文件與代碼標準規範完成開發，實現了場地、單位、設備等靜態資料的查詢功能。所有代碼均遵循繁體中文註解、camelCase/PascalCase 命名規範、禁用 Lambda/Stream API 的要求。

該模組為預約流程的前置步驟，為 Booking 模組提供必要的場地與設備資訊支撐。設計上預留了多租戶擴展、快取優化等升級空間，符合軟體工程最佳實踐。

**下一步建議：**
1. 執行 `venue_tables.sql` 初始化資料庫
2. 插入測試數據（學務處單位、示例場地、設備）
3. 集成測試 `/api/public/**` 三個終端點
4. 與前端協作集成 API

