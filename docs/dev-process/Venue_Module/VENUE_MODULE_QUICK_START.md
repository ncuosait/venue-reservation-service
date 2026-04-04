# Venue 模組快速開始指南

**最後更新：** 2026-04-04  
**適用版本：** Venue Module v1.0

---

## 快速導覽

本文檔幫助開發者快速了解 Venue 模組的結構與使用方式。

---

## 一、模組概要

### 核心職責
- 管理場地（Venues）、組織單位（Units）、設備（Equipments）等靜態資料
- 為預約流程提供場地與設備信息支撐
- 提供公開的 REST API，無需身份驗證

### 核心特性
- ✅ 支持多租戶（多單位）架構設計
- ✅ 避免 N+1 查詢，優化資料庫性能
- ✅ 統一的 Result<T> 回傳格式
- ✅ 完整的 Javadoc 與繁體中文註解

---

## 二、快速開始

### 2.1 初始化資料庫

執行 `venue_tables.sql` 建立必要的表結構：

```bash
mysql -u root -p venue_reservation_system < venue_tables.sql
```

### 2.2 插入測試數據

在 MySQL 中執行以下 SQL，建立測試數據：

```sql
USE venue_reservation_system;

-- 1. 插入管理單位
INSERT INTO units (name, code) VALUES 
('學務處本部', 'OSA'),
('住宿服務組', 'HSL'),
('課外活動組', 'CEA');

-- 2. 插入設備
INSERT INTO equipments (name) VALUES
('投影機'),
('無線麥克風'),
('音響系統'),
('白板'),
('桌椅組');

-- 3. 插入場地（假設學務處 ID 為 1）
INSERT INTO venues (unit_id, name, capacity, description) VALUES
(1, '會議室 A', 20, '小型會議場所，配備投影機和白板'),
(1, '會議室 B', 50, '中型會議場所，配備音響系統'),
(1, '大禮堂', 200, '大型集會場所，可容納 200 人');

-- 4. 配置場地設備（假設會議室 A ID 為 1）
INSERT INTO venue_equipment_map (venue_id, equipment_id) VALUES
(1, 1),  -- 會議室 A + 投影機
(1, 2),  -- 會議室 A + 無線麥克風
(2, 3),  -- 會議室 B + 音響系統
(3, 1),  -- 大禮堂 + 投影機
(3, 3);  -- 大禮堂 + 音響系統
```

### 2.3 啟動應用

```bash
mvn spring-boot:run
```

### 2.4 測試 API

#### 取得所有單位
```bash
curl http://localhost:8080/api/public/units
```

期望回應：
```json
{
  "success": true,
  "message": "操作成功",
  "data": [
    { "id": 1, "name": "學務處本部", "code": "OSA" },
    { "id": 2, "name": "住宿服務組", "code": "HSL" },
    { "id": 3, "name": "課外活動組", "code": "CEA" }
  ]
}
```

#### 查詢單位下的場地
```bash
curl http://localhost:8080/api/public/venues?unitId=1
```

期望回應：
```json
{
  "success": true,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "unitId": 1,
      "name": "會議室 A",
      "capacity": 20,
      "description": "小型會議場所，配備投影機和白板",
      "equipments": [
        { "id": 1, "name": "投影機" },
        { "id": 2, "name": "無線麥克風" }
      ]
    },
    {
      "id": 2,
      "unitId": 1,
      "name": "會議室 B",
      "capacity": 50,
      "description": "中型會議場所，配備音響系統",
      "equipments": [
        { "id": 3, "name": "音響系統" }
      ]
    }
  ]
}
```

#### 查詢場地詳情
```bash
curl http://localhost:8080/api/public/venues/1
```

期望回應：
```json
{
  "success": true,
  "message": "操作成功",
  "data": {
    "id": 1,
    "unitId": 1,
    "name": "會議室 A",
    "capacity": 20,
    "description": "小型會議場所，配備投影機和白板",
    "equipments": [
      { "id": 1, "name": "投影機" },
      { "id": 2, "name": "無線麥克風" }
    ]
  }
}
```

---

## 三、架構概覽

### 3.1 分層設計

```
┌─────────────────────────────────────┐
│   Controller Layer                   │
│   (VenueController)                 │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│   Service Layer                      │
│   (VenueService / VenueServiceImpl)  │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│   Mapper/Persistence Layer           │
│   (UnitMapper, VenueMapper, ...)    │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│   Database                           │
│   (MySQL with units, venues, ...)   │
└─────────────────────────────────────┘
```

### 3.2 數據流向

```
前端請求
  ↓
VenueController (接收請求，調用 Service)
  ↓
VenueService (調用 Mapper，進行數據轉換)
  ↓
Mapper (執行 SQL 查詢，返回 Entity)
  ↓
MyBatis (ResultMap 映射，自動填充關聯)
  ↓
Database (執行 SQL，返回行集)
  ↓
VenueService (Entity 轉換為 VO)
  ↓
VenueController (包裝 Result<T>，返回前端)
```

---

## 四、核心組件說明

### 4.1 Entity 實體
- **Unit.java** - 對應 `units` 表，代表單位
- **Venue.java** - 對應 `venues` 表，代表場地（包含 equipments 集合）
- **Equipment.java** - 對應 `equipments` 表，代表設備

### 4.2 Mapper 持久層
- **UnitMapper.java** - 單位查詢
- **VenueMapper.java** - 場地查詢（含設備關聯）
- **EquipmentMapper.java** - 設備查詢

### 4.3 Service 業務層
- **VenueService（介面）** - 定義業務契約
- **VenueServiceImpl（實作）** - 實現業務邏輯
  - `getAllUnits()` - 獲取所有單位
  - `getVenuesByUnitId(Long)` - 根據單位查詢場地
  - `getVenueById(Long)` - 查詢單一場地詳情

### 4.4 VO 傳輸物件
- **UnitVO** - 單位輸出物件
- **VenueVO** - 場地輸出物件
- **EquipmentVO** - 設備輸出物件

### 4.5 Controller 控制層
- **VenueController** - 公開 API 終端點
  - `GET /api/public/units`
  - `GET /api/public/venues?unitId={id}`
  - `GET /api/public/venues/{id}`

---

## 五、常見問題（FAQ）

### Q1: 為什麼使用 `/api/public/` 路徑？

**A:** 場地資訊是公開信息，使用者應在登入前即可瀏覽，以便決定是否進行預約。同時這樣的設計避免不必要的認證開銷，提升性能。

### Q2: 為什麼設備查詢通過 LEFT JOIN 而不是分開查詢？

**A:** 使用 LEFT JOIN 避免 N+1 查詢問題。一次 SQL 獲取場地與設備，而非針對每個場地分別查詢設備，大幅提升性能。

### Q3: 如何擴展支援更多單位？

**A:** 只需在 `units` 表插入新記錄，系統會自動支持。無需修改任何代碼邏輯。

### Q4: 場地沒有設備時會怎樣？

**A:** LEFT JOIN 確保即使場地無設備，也能正確返回場地信息，`equipments` 為空列表。

### Q5: 能否增加快取機制？

**A:** 完全可以。Service 層方法使用 `@Cacheable` 註解，建議對 `getAllUnits()` 和 `getVenueById()` 加上 Redis 快取。

---

## 六、性能優化建議

### 6.1 數據庫層
- ✅ 已使用索引：`idx_unit_id`、`uk_venue_equip`
- ✅ 已避免 N+1：使用 LEFT JOIN 與 ResultMap
- 📌 建議：監控 `selectVenuesByUnitId()` 的執行時間

### 6.2 應用層
- 📌 建議：引入 Redis 快取場地與單位信息
- 📌 建議：對高頻 API 添加速率限制（Rate Limiting）

### 6.3 前端層
- 📌 建議：快取已加載的場地列表，避免重複請求
- 📌 建議：實現樂觀更新（Optimistic Update）

---

## 七、開發建議

### 7.1 添加新 API
如需添加新的查詢接口（如按容量搜尋場地）：

1. **修改 Mapper：** 在 `VenueMapper.xml` 添加新 SQL
2. **修改 Service：** 在 `VenueService` 添加方法簽名，在 `VenueServiceImpl` 實作
3. **修改 Controller：** 添加對應的 `@GetMapping` 方法

### 7.2 修改數據結構
如需添加場地新欄位（如建築代號）：

1. **修改資料庫：** `ALTER TABLE venues ADD COLUMN building_code VARCHAR(10);`
2. **修改 Entity：** 在 `Venue.java` 添加新字段
3. **修改 XML 映射：** 在 `VenueMapper.xml` 的 SELECT 添加新欄位
4. **修改 VO：** 在 `VenueVO.java` 添加新字段（如需暴露給 API）
5. **修改 Service：** 在 `convertVenueToVO()` 填充新字段

---

## 八、故障排除

### 問題：查詢場地返回空列表
**排查步驟：**
1. 確認數據庫已初始化：`SELECT COUNT(*) FROM venues;`
2. 確認 unitId 正確：`SELECT * FROM venues WHERE unit_id = 1;`
3. 檢查 Mapper XML 的 WHERE 條件

### 問題：設備清單為空
**排查步驟：**
1. 檢查 `venue_equipment_map` 表：`SELECT * FROM venue_equipment_map WHERE venue_id = 1;`
2. 確認設備存在：`SELECT * FROM equipments;`
3. 檢查 LEFT JOIN 邏輯

### 問題：500 Internal Server Error
**排查步驟：**
1. 檢查應用日誌：`tail -f logs/application.log`
2. 確認資料庫連接：`SELECT 1 FROM units;`
3. 確認 MyBatis 配置正確

---

## 九、相關文件清單

| 文檔 | 位置 | 用途 |
| :--- | :--- | :--- |
| 設計文件 | `docs/Design/...` | 理解設計意圖 |
| 完成報告 | `docs/dev-process/VENUE_MODULE_COMPLETION_REPORT.md` | 開發成果總結 |
| 檢查清單 | `docs/dev-process/VENUE_MODULE_CHECKLIST.md` | 代碼品質驗證 |
| 本指南 | `docs/dev-process/VENUE_MODULE_QUICK_START.md` | 快速上手 |

---

## 十、下一步

1. ✅ 初始化資料庫並插入測試數據
2. ✅ 啟動應用並測試 API
3. ✅ 與前端整合 API
4. ⏳ 添加更多功能（如設備庫存追蹤）
5. ⏳ 引入快取機制優化性能
6. ⏳ 實現多單位狀態管理

---

**需要幫助？** 參考設計文件或完成報告中的詳細說明。

