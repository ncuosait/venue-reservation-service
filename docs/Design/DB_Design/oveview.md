# 場地租借系統資料庫設計文件 (Database Design Document)

**日期：** 2026-03-18  
**版本號：** V1.0 (MVP)  

---

## 一、 整體設計理念與策略

### 1. 正規化策略 (Normalization)
本設計遵循 **第三正規化 (3NF)**，確保資料的原子性並消除過度的冗餘。
* 將「管理單位」與「場地」解耦，支援多單位擴展。
* 將「預約申請」與「借用設備」分離，處理一對多關係，避免在主表中使用違反 1NF 的格式。

### 2. 位元遮罩 (Bitmasking) 時段管理
為了解決預約系統最頭痛的「時段衝突檢查」，我們不採用多筆記錄存儲時段，而是將一天 24 小時拆分為 12 個 2 小時的區間，以一個 **12-bit 的整數** (`Integer`) 儲存。
* **優勢：** 衝突檢查只需執行一次位元運算 `(existing_mask & new_mask) != 0`，極大提升查詢效率。

### 3. 索引設計理念
* **複合索引 (Composite Index)：** 針對 `(venue_id, booking_date)` 建立複合索引。
* **原因：** 前台月曆與後台管理介面最頻繁的查詢是「查詢某場地在某個日期區間的所有預約」，該索引能確保月曆載入速度達到毫秒等級。

### 4. 併發與鎖設計 (Locking)
* **樂觀鎖 (Optimistic Locking)：** 在 `Bookings` 表引入 `version` 欄位。
* **原因：** 場地預約屬於「讀多寫少」但「寫入瞬間高度競爭」的場景。使用樂觀鎖可避免悲觀鎖造成的資料庫連線長時間佔用，並確保不會發生「兩個人同時訂到同一時段」的超賣情況。

---

## 二、 資料表詳細設計

### 1. 表名：`units` (管理單位)
| 欄位名 | 類型 | 約束 | 說明 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, Auto Inc | 單位唯一識別碼 |
| `name` | VARCHAR(50) | Not Null, Unique | 單位名稱 (如：學務處本部) |
| `code` | VARCHAR(10) | Not Null, Unique | 單位代碼 (如：STUA)，對接 Portal 身分用 |

### 2. 表名：`venues` (場地資訊)
| 欄位名 | 類型 | 約束 | 說明 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, Auto Inc | 場地唯一識別碼 |
| `unit_id` | BIGINT | FK (units.id) | 所屬單位，索引關鍵字 |
| `name` | VARCHAR(50) | Not Null | 場地名稱 (如：會議室 A) |
| `capacity` | INT | Not Null | 容納人數上限 |
| `description` | TEXT | Nullable | 借用規則或場地介紹 |

### 3. 表名：`bookings` (預約申請)
| 欄位名 | 類型 | 約束 | 說明 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, Auto Inc | 申請案編號 |
| `venue_id` | BIGINT | FK (venues.id) | 關聯場地 |
| `user_id` | VARCHAR(20) | Not Null | 申請人 NCU Portal ID |
| `booking_date` | DATE | Not Null | 預約日期 |
| `time_slots` | INT | Not Null | 12-bit 位元遮罩時段 |
| `status` | TINYINT | Default 1 | 0:撤回, 1:審核中, 2:通過, 3:拒絕 |
| `purpose` | VARCHAR(255) | Not Null | 使用用途 |
| `p_count` | INT | Not Null | 預估人數 |
| `contact_info` | JSON | Not Null | 包含聯絡人姓名、電子郵件等 |
| `version` | INT | Default 1 | 樂觀鎖版本號 |

### 4. 表名：`booking_equipment` (借用設備)
| 欄位名 | 類型 | 約束 | 說明 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, Auto Inc | 唯一識別碼 |
| `booking_id` | BIGINT | FK (bookings.id) | 關聯的預約申請 |
| `equip_name` | VARCHAR(50) | Not Null | 設備名稱 (如：麥克風) |

### 5. 表名：`audit_logs` (稽核日誌)
| 欄位名 | 類型 | 約束 | 說明 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, Auto Inc | 紀錄編號 |
| `booking_id` | BIGINT | Index | 關聯預約單號 |
| `op_id` | VARCHAR(20) | Not Null | 操作者 ID |
| `action` | VARCHAR(20) | Not Null | 動作類型 (如：UPDATE_STATUS) |
| `old_data` | JSON | Nullable | 變更前快照 |
| `new_data` | JSON | Nullable | 變更後快照 |
| `created_at` | TIMESTAMP | Default NOW | 發生時間 |

---

## 三、 風險分析：若不依據此設計可能產生的問題

1.  **若不實作 `unit_id` 多租戶隔離：**
    未來整合住服組或課外組時，必須為每個單位重新寫一套代碼或開新資料庫，維運成本成倍增長，且無法實現「一份代碼服務全校」的軟體工程目標。

2.  **若不採用 Bitmasking 處理時段：**
    判斷時段衝突必須從 DB 撈出多筆記錄並在 Java 進行複雜的雙重循環比對。當場地數量增加，月曆載入會明顯卡頓，且程式碼會充斥難以維護的 `if-else`。

3.  **若不使用樂觀鎖 (Version)：**
    在高併發情況下（例如熱門時段開放申請瞬間），極可能發生 **Race Condition**。兩筆請求同時通過後端的檢查，導致資料庫存入兩筆重疊的預約，這對學校行政作業是嚴重的事故。

4.  **若無 `audit_logs` 稽核日誌：**
    管理員若誤刪或誤改學生申請，系統將無法追蹤是誰操作、何時操作。在公部門或校園行政系統中，缺乏「操作不可否認性」會導致系統的可信度大打折扣。

---


