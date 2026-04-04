# Restful api design

## 🛠️ 第一階段：場地與基礎數據 API (Metadata & Availability)

這部分的設計思想在於提供前端渲染「場地月曆」所需的靜態與動態數據。

### 1. 取得管理單位列表

- **端點：** `GET /api/units`
- **功能：** 讓使用者選擇要預約哪一個單位（如：學務處、住服組）。
- **成功回傳 (200 OK)：**
  ```json
  {
    "success": true,
    "message": "操作成功",
    "data": [
      { "id": 1, "name": "學務處本部", "code": "OSA" },
      { "id": 2, "name": "住宿服務組", "code": "HSD" }
    ]
  }
  ```

### 2. 根據單位取得場地列表

- **端點：** `GET /api/venues`
- **參數：** `unitId` (Query, Long, 必填)
- **成功回傳 (200 OK)：**
  ```json
  {
    "success": true,
    "message": "操作成功",
    "data": [
      {
        "id": 101,
        "name": "會議室 A",
        "capacity": 20,
        "description": "配備投影機與無線麥克風"
      }
    ]
  }
  ```
- **失敗回傳 (400 Bad Request - 缺少參數)：**
  ```json
  {
    "success": false,
    "message": "Required parameter 'unitId' is not present",
    "data": null
  }
  ```

### 3. 取得場地月曆佔用狀態 (關鍵核心)

- **端點：** `GET /api/venues/{id}/availability`
- **路徑參數：** `id` (場地 ID)
- **查詢參數：** `month` (String, 格式 `YYYY-MM`, 必填)
- **設計說明：** 後端會過濾出該月份所有 **狀態為 APPROVED** 的申請案，並將同日期的時段進行位元 `OR` 運算，回傳當天總佔用遮罩。
- **成功回傳 (200 OK)：**
  ```json
  {
    "success": true,
    "message": "操作成功",
    "data": {
      "2026-03-01": 0, // 0 代表全天可借
      "2026-03-02": 768, // 08:00-10:00 已被佔用 (2^8 + 2^9)
      "2026-03-03": 16777215 // 16777215 = 2^24 - 1 (全天已被佔用)
    }
  }
  ```
- **失敗回傳 (404 Not Found - 場地不存在)：**
  ```json
  {
    "success": false,
    "message": "找不到該場地資訊",
    "data": null
  }
  ```

---

## 🛠️ 第二階段：使用者預約操作 API (Booking Operations)

這部分處理申請的生命週期，包含提交、查詢與撤回。

### 4. 提交預約申請

- **端點：** `POST /api/bookings`
- **接收 JSON (DTO)：**
  ```json
  {
    "venueId": 101,
    "bookingDate": "2026-03-27",
    "slots": [8, 9, 10],
    "purpose": "專案小組週會",
    "participantCount": 5,
    "contactInfo": {
      "name": "王小明",
      "email": "student@ncu.edu.tw",
      "phone": "0912345678"
    },
    "equipmentNames": ["麥克風", "白板筆"]
  }
  ```
- **衝突檢查成功 (200 OK)：**
  ```json
  {
    "success": true,
    "message": "申請已送出，請靜候管理員審核",
    "data": { "bookingId": 501 }
  }
  ```
- **時段衝突失敗 (200 OK - 業務錯誤)：**
  ```json
  {
    "success": false,
    "message": "該時段已被其他已通過之申請佔用",
    "data": null
  }
  ```

### 5. 查看個人申請清單

- **端點：** `GET /api/my/bookings`
- **成功回傳 (200 OK)：**
  ```json
  {
    "success": true,
    "message": "操作成功",
    "data": [
      {
        "id": 501,
        "venueName": "會議室 A",
        "bookingDate": "2026-03-27",
        "slots": [8, 9, 10],
        "status": 1, // 1: 審核中
        "createdAt": "2026-03-20 10:00:00"
      }
    ]
  }
  ```

### 6. 撤回申請

- **端點：** `PUT /api/bookings/{id}/withdraw`
- **說明：** 僅限狀態為「審核中」或「已通過」的案件可撤回。
- **成功回傳 (200 OK)：**
  ```json
  {
    "success": true,
    "message": "申請已撤回",
    "data": null
  }
  ```

---

## 🛠️ 第三階段：管理員審核與稽核 API (Admin & Audit)

### 7. 管理員視角：月曆詳細清單

- **端點：** `GET /api/admin/bookings/calendar`
- **功能：** 供管理員在月曆介面查看所屬單位的所有申請案（包含 Pending, Approved, Rejected）。
- **查詢參數：** `unitId` (Long, 必填), `month` (String, `YYYY-MM`, 必填)
- **設計說明：** 不同於前台只回傳遮罩（Mask），此 API 回傳詳細列表，讓管理員點擊月曆格子時能看到是誰借的、要做什麼。
- **成功回傳 (200 OK)：**
  ```json
  {
    "success": true,
    "message": "操作成功",
    "data": [
      {
        "id": 501,
        "userName": "張小明",
        "venueName": "會議室 A",
        "bookingDate": "2026-03-27",
        "slots": [8, 9, 10],
        "status": 1,
        "purpose": "資管系專題討論"
      }
    ]
  }
  ```

### 8. 審核申請案 (通過或拒絕)

- **端點：** `POST /api/admin/bookings/{id}/audit`
- **路徑參數：** `id` (申請案 ID)
- **接收 JSON (DTO)：**
  ```json
  {
    "status": 2, // 2: APPROVED, 3: REJECTED
    "adminRemark": "准予借用，請注意環境整潔"
  }
  ```
- **設計說明：** 後端在更新狀態的同時，必須觸發發送郵件通知，並將此審核動作紀錄至 `Audit_Logs`。
- **成功回傳 (200 OK)：**
  ```json
  {
    "success": true,
    "message": "審核完成",
    "data": null
  }
  ```

### 9. 管理員強制修改預約 (變更時段或資訊)

- **端點：** `PUT /api/admin/bookings/{id}`
- **功能：** 管理員可直接修改任何狀態的申請，例如協助學生微調時段。
- **接收 JSON (DTO)：**
  ```json
  {
    "bookingDate": "2026-03-27",
    "slots": [10, 11],
    "venueId": 101
  }
  ```
- **設計說明：** 這是最高權限操作，**必須**強制記錄修改前與修改後的 `old_data` 與 `new_data`。
- **成功回傳 (200 OK)：**
  ```json
  {
    "success": true,
    "message": "修改成功並已存入稽核日誌",
    "data": null
  }
  ```

### 10. 查詢稽核日誌 (Audit Logs)

- **端點：** `GET /api/admin/audit-logs`
- **功能：** 供系統管理員查詢所有管理動作紀錄。
- **參數：** `bookingId` (Query, 可選), `page` (Query, 預設 1)
- **成功回傳 (200 OK)：**
  ```json
  {
    "success": true,
    "message": "操作成功",
    "data": {
      "list": [
        {
          "id": 99,
          "operatorName": "學務處管理員-王大同",
          "action": "UPDATE_STATUS",
          "oldValue": "PENDING",
          "newValue": "APPROVED",
          "createdAt": "2026-03-27 10:10:00"
        }
      ],
      "total": 1
    }
  }
  ```

### 11. 管理員白名單管理 (Super Admin Only)

- **端點：** `POST /api/admin/white-list`
- **功能：** 增加或移除擁有管理權限的用戶。
- **接收 JSON：**
  ```json
  {
    "userId": "staff001",
    "unitId": 1,
    "action": "ADD" // ADD 或 REMOVE
  }
  ```
