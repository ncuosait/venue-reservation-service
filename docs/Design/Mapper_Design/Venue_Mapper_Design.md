# 場地與組織模組：模組 Mapper 設計文件 (V1.0)

**日期：** 2026-04-04  
**類別：** - `tw.edu.ncu.osa.venue_reservation_service.mapper.UnitMapper`
- `tw.edu.ncu.osa.venue_reservation_service.mapper.VenueMapper`
- `tw.edu.ncu.osa.venue_reservation_service.mapper.EquipmentMapper`

## 一、 設計概述

Mapper 層負責將場地、單位及設備的資料庫紀錄轉化為 Java 實體物件。本模組的設計重點在於處理「場地與設備」之間的一對多（或多對多）關聯查詢。為了優化前端載入效能，SQL 實作上會優先考慮使用連接查詢（Join）一次性抓取關聯數據，並確保查詢邏輯嚴格遵循資料庫索引規範。

## 二、 Mapper 介面定義

### 1. UnitMapper.java
| 方法名稱 | 參數 | 回傳值 | 功能描述 |
| :--- | :--- | :--- | :--- |
| `selectAllUnits` | 無 | `List<Unit>` | 查詢所有管理單位紀錄。 |

### 2. VenueMapper.java
| 方法名稱 | 參數 | 回傳值 | 功能描述 |
| :--- | :--- | :--- | :--- |
| `selectVenuesByUnitId` | `unitId` | `List<Venue>` | 根據單位 ID 查詢所屬場地列表，並包含設備清單。 |
| `selectVenueById` | `id` | `Venue` | 根據主鍵查詢場地詳情及其可借用設備。 |

### 3. EquipmentMapper.java
| 方法名稱 | 參數 | 回傳值 | 功能描述 |
| :--- | :--- | :--- | :--- |
| `selectEquipmentsByVenueId` | `venueId` | `List<Equipment>` | 查詢特定場地在 `venue_equipment_map` 中關聯的所有設備。 |

## 三、 SQL 映射規範 (XML)

### 1. 單位清單查詢 (`UnitMapper.xml`)
```xml
<select id="selectAllUnits" resultType="tw.edu.ncu.osa.venue_reservation_service.model.entity.Unit">
    SELECT id, name, code FROM units ORDER BY id ASC
</select>
```

### 2. 場地與設備關聯查詢 (`VenueMapper.xml`)
為了避免 N+1 查詢問題，場地資訊會透過 `ResultMap` 與 `collection` 進行聯表封裝：

```xml
<resultMap id="VenueDetailMap" type="tw.edu.ncu.osa.venue_reservation_service.model.entity.Venue">
    <id property="id" column="id" />
    <result property="unitId" column="unit_id" />
    <result property="name" column="name" />
    <result property="capacity" column="capacity" />
    <result property="description" column="description" />
    <collection property="equipments" ofType="tw.edu.ncu.osa.venue_reservation_service.model.entity.Equipment">
        <id property="id" column="equip_id" />
        <result property="name" column="equip_name" />
    </collection>
</resultMap>

<select id="selectVenuesByUnitId" resultMap="VenueDetailMap">
    SELECT 
        v.*, 
        e.id as equip_id, 
        e.name as equip_name
    FROM venues v
    LEFT JOIN venue_equipment_map vem ON v.id = vem.venue_id
    LEFT JOIN equipments e ON vem.equipment_id = e.id
    WHERE v.unit_id = #{unitId}
</select>

<select id="selectVenueById" resultMap="VenueDetailMap">
    SELECT 
        v.*, 
        e.id as equip_id, 
        e.name as equip_name
    FROM venues v
    LEFT JOIN venue_equipment_map vem ON v.id = vem.venue_id
    LEFT JOIN equipments e ON vem.equipment_id = e.id
    WHERE v.id = #{id}
</select>
```

### 3. 設備資料檢索 (`EquipmentMapper.xml`)
```xml
<select id="selectEquipmentsByVenueId" resultType="tw.edu.ncu.osa.venue_reservation_service.model.entity.Equipment">
    SELECT e.* FROM equipments e
    JOIN venue_equipment_map vem ON e.id = vem.equipment_id
    WHERE vem.venue_id = #{venueId}
</select>
```

## 四、 數據映射與性能考量

### 1. 索引利用
* **單位過濾**：場地查詢 SQL 嚴格使用 `idx_unit_id` 索引，確保根據單位篩選時具備高效能。
* **關聯查詢**：設備關聯查詢利用 `venue_equipment_map` 表的唯一索引 `uk_venue_equip`，提升多表 Join 的速度。

### 2. 映射策略
* **Left Join 使用**：在查詢場地時採用 `LEFT JOIN`，確保即使場地目前沒有配置任何可借用設備，仍能正確回傳場地的基礎資訊。
* **ResultMap 複用**：透過定義 `VenueDetailMap`，統一處理列表查詢與單筆詳細查詢的物件封裝邏輯，維持代碼的一致性。

### 3. 資料庫欄位處理
* **Description**：資料庫中的 `TEXT` 欄位 映射為 Java 的 `String`。
* **自增 ID**：所有的 `INSERT` 操作（若後續需要管理功能）都應回填 `id` 主鍵。