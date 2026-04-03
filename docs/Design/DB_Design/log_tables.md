# 稽核紀錄表格

```sql
-- 設計理念：記錄管理行為快照。
CREATE TABLE `audit_logs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '紀錄編號',
    `booking_id` BIGINT DEFAULT NULL COMMENT '關聯預約單號',
    `op_id` VARCHAR(20) NOT NULL COMMENT '操作者 ID',
    `action` VARCHAR(20) NOT NULL COMMENT '動作類型 (如：UPDATE_STATUS)',
    `old_data` JSON DEFAULT NULL COMMENT '變更前資料快照',
    `new_data` JSON DEFAULT NULL COMMENT '變更後資料快照',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '發生時間',
    INDEX `idx_booking_id` (`booking_id`),
    INDEX `idx_op_id` (`op_id`)
) ENGINE=INNODB COMMENT='稽核日誌表';
```