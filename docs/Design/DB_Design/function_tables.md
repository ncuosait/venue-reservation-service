# 業務資料表設計

```sql
-- 1. 管理單位表 (Units)
-- 設計理念：支援多租戶擴展，區分不同行政單位。
CREATE TABLE `units` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '單位唯一識別碼',
    `name` VARCHAR(50) NOT NULL UNIQUE COMMENT '單位名稱 (如：學務處本部)',
    `code` VARCHAR(10) NOT NULL UNIQUE COMMENT '單位代碼 (如：STUA)，對接 Portal 身分用',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=INNODB COMMENT='管理單位表';

-- 2. 場地資訊表 (Venues)
-- 設計理念：透過 unit_id 索引優化查詢效能。
CREATE TABLE `venues` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '場地唯一識別碼',
    `unit_id` BIGINT NOT NULL COMMENT '所屬單位 ID',
    `name` VARCHAR(50) NOT NULL COMMENT '場地名稱 (如：會議室 A)',
    `capacity` INT NOT NULL COMMENT '容納人數上限',
    `description` TEXT DEFAULT NULL COMMENT '借用規則或場地介紹',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_venue_unit` FOREIGN KEY (`unit_id`) REFERENCES `units` (`id`) ON DELETE CASCADE,
    INDEX `idx_unit_id` (`unit_id`)
) ENGINE=INNODB COMMENT='場地資訊表';

-- 3. 設備主表 (Equipments)
-- 設計理念：存放全校/全單位通用的設備清單，避免在各場地重複儲存字串。
CREATE TABLE `equipments` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '設備唯一識別碼',
    `name` VARCHAR(50) NOT NULL UNIQUE COMMENT '設備名稱 (如：無線麥克風)',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=INNODB COMMENT='設備主表';

-- 4. 場地可供借用設備表 (Venue_Equipment_Map)
-- 設計理念：定義 M:N 關係，標註「哪個場地擁有哪個設備」。
CREATE TABLE `venue_equipment_map` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `venue_id` BIGINT NOT NULL COMMENT '關聯場地',
    `equipment_id` BIGINT NOT NULL COMMENT '關聯設備',
    CONSTRAINT `fk_map_venue` FOREIGN KEY (`venue_id`) REFERENCES `venues` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_map_equip` FOREIGN KEY (`equipment_id`) REFERENCES `equipments` (`id`) ON DELETE CASCADE,
    UNIQUE INDEX `uk_venue_equip` (`venue_id`, `equipment_id`)
) ENGINE=INNODB COMMENT='場地與可借用設備對照表';

-- 5. 預約申請表 (Bookings)
-- 設計理念：樂觀鎖 (version) 確保併發安全，複合索引優化月曆渲染。
CREATE TABLE `bookings` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '申請案編號',
    `venue_id` BIGINT NOT NULL COMMENT '關聯場地 ID',
    `user_id` VARCHAR(20) NOT NULL COMMENT '申請人 NCU Portal ID',
    `booking_date` DATE NOT NULL COMMENT '預約日期',
    `time_slots` INT UNSIGNED NOT NULL COMMENT '24-bit 位元遮罩時段 (1小時一格)',
    `status` TINYINT DEFAULT 1 NOT NULL COMMENT '0:撤回, 1:審核中, 2:通過, 3:拒絕',
    `purpose` VARCHAR(255) NOT NULL COMMENT '使用用途',
    `p_count` INT NOT NULL COMMENT '預估人數',
    `contact_info` JSON NOT NULL COMMENT '聯絡人姓名、電子郵件等 JSON 資訊',
    `version` INT DEFAULT 1 NOT NULL COMMENT '樂觀鎖版本號',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_booking_venue` FOREIGN KEY (`venue_id`) REFERENCES `venues` (`id`) ON DELETE CASCADE,
    INDEX `idx_venue_date` (`venue_id`, `booking_date`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=INNODB COMMENT='預約申請表';

-- 6. 該筆預約實際借用設備表 (Booking_Equipment)
-- 設計理念：記錄特定預約中選用的設備。
CREATE TABLE `booking_equipment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '唯一識別碼',
    `booking_id` BIGINT NOT NULL COMMENT '關聯預約 ID',
    `equipment_id` BIGINT NOT NULL COMMENT '關聯設備 ID',
    CONSTRAINT `fk_be_booking` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_be_equip` FOREIGN KEY (`equipment_id`) REFERENCES `equipments` (`id`) ON DELETE CASCADE
) ENGINE=INNODB COMMENT='該筆預約借用設備紀錄表';
```
