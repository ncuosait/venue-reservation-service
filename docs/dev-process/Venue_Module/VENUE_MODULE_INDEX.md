# Venue 模組開發索引

**模組狀態：** ✅ 開發完成 (v1.0)  
**最後更新：** 2026-04-04

---

## 一、模組概述

**場地與組織模組** 負責管理系統中的靜態資料，包括：
- 管理單位（Units）
- 場地資訊（Venues）
- 設備配置（Equipments & Venue-Equipment Mapping）

該模組為預約流程（Booking 模組）的前置支撐，提供公開的 REST API 供前端查詢場地信息。

---

## 二、快速導航

### 📚 設計文件
| 文檔 | 內容 | 位置 |
| :--- | :--- | :--- |
| **Module 設計** | 模組功能、職責、擴展策略 | `docs/Design/Module_Design/Venue_Module_Design.md` |
| **API 設計** | REST 終端點、VO 結構、請求/響應格式 | `docs/Design/API_Design/Venue_API_Design.md` |
| **Mapper 設計** | SQL 映射、性能優化、N+1 避免方案 | `docs/Design/Mapper_Design/Venue_Mapper_Design.md` |
| **Service 設計** | 業務邏輯、異常處理、事務策略 | `docs/Design/Service_design/Venue_Service_Design.md` |
| **資料庫設計** | 表結構、索引、外鍵、約束 | `docs/Design/DB_Design/function_tables.md` |

### 💻 實現文件

#### Entity 實體層
| 文件 | 職責 | 對應表 |
| :--- | :--- | :--- |
| `Unit.java` | 單位實體 | `units` |
| `Venue.java` | 場地實體（含設備集合） | `venues` |
| `Equipment.java` | 設備實體 | `equipments` |

路徑：`src/main/java/tw/edu/ncu/osa/venue_reservation_service/model/entity/`

#### Mapper 持久層

**Java 介面：**
| 文件 | 方法 | 說明 |
| :--- | :--- | :--- |
| `UnitMapper.java` | `selectAllUnits()` | 查詢所有單位 |
| `VenueMapper.java` | `selectVenuesByUnitId()` | 根據單位查詢場地（含設備） |
| | `selectVenueById()` | 根據場地 ID 查詢詳情 |
| `EquipmentMapper.java` | `selectEquipmentsByVenueId()` | 根據場地查詢設備 |

路徑：`src/main/java/tw/edu/ncu/osa/venue_reservation_service/mapper/`

**XML 映射：**
| 文件 | SQL 內容 |
| :--- | :--- |
| `UnitMapper.xml` | 簡單 SELECT |
| `VenueMapper.xml` | LEFT JOIN + ResultMap + collection |
| `EquipmentMapper.xml` | JOIN 查詢 |

路徑：`src/main/resources/mapper/`

#### Service 業務層
| 文件 | 職責 |
| :--- | :--- |
| `VenueService.java` | 介面定義 |
| `VenueServiceImpl.java` | 實現，包含 Entity-to-VO 轉換 |

路徑：`src/main/java/tw/edu/ncu/osa/venue_reservation_service/service/`

#### VO 傳輸層
| 文件 | 用途 |
| :--- | :--- |
| `UnitVO.java` | 單位 API 回傳 |
| `VenueVO.java` | 場地 API 回傳（含設備） |
| `EquipmentVO.java` | 設備 API 回傳 |

路徑：`src/main/java/tw/edu/ncu/osa/venue_reservation_service/model/vo/`

#### Controller 控制層
| 文件 | 功能 |
| :--- | :--- |
| `VenueController.java` | 3 個公開 API 終端點 |

路徑：`src/main/java/tw/edu/ncu/osa/venue_reservation_service/controller/`

### 📋 開發文檔

| 文檔 | 用途 | 位置 |
| :--- | :--- | :--- |
| **完成報告** | 開發成果、代碼品質檢查、架構決策 | `docs/dev-process/VENUE_MODULE_COMPLETION_REPORT.md` |
| **檢查清單** | 架構驗證、代碼標準、功能完整性 | `docs/dev-process/VENUE_MODULE_CHECKLIST.md` |
| **快速開始** | 初始化、測試 API、常見問題 | `docs/dev-process/VENUE_MODULE_QUICK_START.md` |
| **本索引** | 文件導航、快速參考 | `docs/dev-process/VENUE_MODULE_INDEX.md` |

---

## 三、API 快速參考

### 公開終端點（無需身份驗證）

```
GET /api/public/units
  └─ 獲取所有管理單位清單
  
GET /api/public/venues?unitId={id}
  └─ 根據單位 ID 查詢場地清單（包含設備）
  
GET /api/public/venues/{id}
  └─ 根據場地 ID 查詢詳細資訊（包含設備）
```

### 回傳格式

所有 API 均回傳統一的 `Result<T>` 格式：

```json
{
  "success": boolean,      // 操作是否成功
  "message": string,       // 提示訊息
  "data": T                // 實際數據（泛型）
}
```

---

## 四、核心設計決策

| 決策 | 原因 | 影響 |
| :--- | :--- | :--- |
| 使用 `/api/public/` 路徑 | 場地信息公開，無需認證 | 提升性能，改善用戶體驗 |
| MyBatis ResultMap + LEFT JOIN | 避免 N+1 查詢 | 單次 SQL 返回場地與設備 |
| 傳統迴圈替代 Stream API | 遵循代碼標準 | 易除錯，符合團隊規範 |
| Entity-VO 分離 | 業務邏輯隔離 | 靈活修改 API 結構 |
| 唯讀事務 (@Transactional) | 優化性能 | 資料庫自動優化隻讀查詢 |

---

## 五、開發檢查清單

### ✅ 已完成項目
- [x] Entity 實體設計（3 個類別）
- [x] Mapper 持久層（3 個 Mapper + 3 個 XML）
- [x] Service 業務層（介面 + 實作）
- [x] VO 傳輸物件（3 個 VO）
- [x] Controller 控制層（3 個 API）
- [x] 代碼標準合規（命名、註解、無 Lambda）
- [x] 資料庫設計（表、索引、外鍵）
- [x] 文檔完整（設計、實現、開發指南）

### ⏳ 建議的後續工作
- [ ] 集成測試（驗證 3 個 API）
- [ ] 性能測試（驗證 LEFT JOIN 效率）
- [ ] 與前端協作整合
- [ ] 引入 Redis 快取
- [ ] 添加多單位狀態過濾
- [ ] 設備庫存追蹤功能

---

## 六、常見操作指南

### 6.1 快速測試

```bash
# 1. 初始化資料庫
mysql -u root -p venue_reservation_system < venue_tables.sql

# 2. 插入測試數據
# 參考 VENUE_MODULE_QUICK_START.md 中的 SQL 腳本

# 3. 啟動應用
mvn spring-boot:run

# 4. 測試 API
curl http://localhost:8080/api/public/units
curl "http://localhost:8080/api/public/venues?unitId=1"
curl http://localhost:8080/api/public/venues/1
```

### 6.2 添加新 API

如需添加新的查詢接口（如按容量搜尋場地）：

**步驟：**
1. 修改 `VenueMapper.java` - 添加新方法簽名
2. 修改 `VenueMapper.xml` - 編寫 SQL
3. 修改 `VenueService.java` - 定義業務方法
4. 修改 `VenueServiceImpl.java` - 實作業務邏輯
5. 修改 `VenueController.java` - 添加 REST 終端點

### 6.3 修改資料結構

如需添加場地新欄位：

**步驟：**
1. 修改資料庫：`ALTER TABLE venues ADD COLUMN ...;`
2. 修改 `Venue.java` - 添加新字段
3. 修改 `VenueMapper.xml` - 更新 SELECT 與 ResultMap
4. 修改 `VenueVO.java` - 如需暴露給 API
5. 修改 `VenueServiceImpl.java` - 更新轉換邏輯

---

## 七、代碼標準速查

### 命名慣例
| 對象 | 規則 | 範例 |
| :--- | :--- | :--- |
| 類別 | PascalCase | `VenueService`、`UnitVO` |
| 方法 | camelCase | `getAllUnits()`、`getVenueById()` |
| 變數 | camelCase | `unitId`、`venueName` |
| 常數 | UPPER_SNAKE_CASE | `MAX_CAPACITY = 500` |

### 註解規範
- ✅ 類別：使用繁體中文 Javadoc
- ✅ 方法：@param、@return 說明
- ✅ 複雜邏輯：使用 `// =====...=====` 分隔符
- ✅ 行內：繁體中文解釋業務邏輯

### 禁止項目
- ❌ Lambda 表達式
- ❌ Stream API (map, filter, forEach 等)
- ❌ 方法引用
- ❌ 空的 catch 區塊

---

## 八、性能優化概覽

| 層級 | 優化手段 | 狀態 |
| :--- | :--- | :--- |
| **資料庫** | 索引（idx_unit_id、uk_venue_equip） | ✅ 已實現 |
| | LEFT JOIN（避免 N+1） | ✅ 已實現 |
| | 預編譯語句（#{} 參數） | ✅ 已實現 |
| **應用** | 唯讀事務優化 | ✅ 已實現 |
| | Entity-VO 分離 | ✅ 已實現 |
| **快取** | Redis 快取 | 📌 建議實現 |
| | 速率限制 | 📌 建議實現 |

---

## 九、故障排除速查表

| 問題 | 可能原因 | 解決方案 |
| :--- | :--- | :--- |
| 場地查詢返回空 | 資料庫未初始化或無數據 | 執行 venue_tables.sql，插入測試數據 |
| 設備清單為空 | venue_equipment_map 無映射 | 檢查關聯表，插入映射數據 |
| 500 錯誤 | Mapper SQL 錯誤 | 檢查 XML 中的欄位名稱和別名 |
| 404 Not Found | 場地 ID 不存在 | 確認 ID 有效，拋出異常被正確處理 |

---

## 十、相關模組與依賴

### 上游依賴
- **基礎設施：** `Result.java`（統一回傳格式）
- **配置：** `WebConfig.java`（排除公開 API 認證）
- **全局異常處理：** `GlobalExceptionHandler`（處理 RuntimeException）

### 下游依賴
- **Booking 模組：** 預約時驗證 venueId 有效性，可調用 `getVenueById()`

### 可選集成
- **快取層：** Redis（優化 getAllUnits 與 getVenueById）
- **日誌層：** 記錄 API 調用與查詢性能
- **監控層：** 追蹤資料庫查詢時間

---

## 十一、聯繫與支援

如有疑問，參考以下資源：

1. **設計文件：** `docs/Design/` - 了解架構意圖
2. **完成報告：** `VENUE_MODULE_COMPLETION_REPORT.md` - 查看實現細節
3. **快速開始：** `VENUE_MODULE_QUICK_START.md` - 動手操作
4. **代碼註解：** 所有文件均有詳細註解，直接閱讀源代碼

---

## 附錄：文件結構樹

```
venue-reservation-service/
├── docs/
│   ├── Design/
│   │   ├── Module_Design/
│   │   │   └── Venue_Module_Design.md          ← 模組設計
│   │   ├── API_Design/
│   │   │   └── Venue_API_Design.md             ← API 設計
│   │   ├── Mapper_Design/
│   │   │   └── Venue_Mapper_Design.md          ← Mapper 設計
│   │   ├── Service_design/
│   │   │   └── Venue_Service_Design.md         ← Service 設計
│   │   └── DB_Design/
│   │       └── function_tables.md              ← 資料庫設計
│   └── dev-process/
│       ├── VENUE_MODULE_COMPLETION_REPORT.md  ← 開發完成報告
│       ├── VENUE_MODULE_CHECKLIST.md           ← 檢查清單
│       ├── VENUE_MODULE_QUICK_START.md         ← 快速開始
│       └── VENUE_MODULE_INDEX.md               ← 本文件
├── src/main/java/tw/edu/ncu/osa/venue_reservation_service/
│   ├── model/
│   │   ├── entity/
│   │   │   ├── Unit.java
│   │   │   ├── Venue.java
│   │   │   └── Equipment.java
│   │   └── vo/
│   │       ├── UnitVO.java
│   │       ├── VenueVO.java
│   │       └── EquipmentVO.java
│   ├── mapper/
│   │   ├── UnitMapper.java
│   │   ├── VenueMapper.java
│   │   └── EquipmentMapper.java
│   ├── service/
│   │   ├── VenueService.java
│   │   └── impl/
│   │       └── VenueServiceImpl.java
│   └── controller/
│       └── VenueController.java
└── src/main/resources/
    └── mapper/
        ├── UnitMapper.xml
        ├── VenueMapper.xml
        └── EquipmentMapper.xml
```

---

**最後一次更新：2026-04-04**  
**開發人員：AI Assistant**  
**狀態：✅ 完成**

