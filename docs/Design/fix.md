## Plan: 月曆視圖返回完整預約資訊方案（重新評估）

直接在 VenueCalendarMonthVO 中嵌入該月的所有 BookingVO 資料，由前端根據各 BookingVO 的 status 和 slots 判斷時段占用，無需後端預先處理時段資訊。

### 方案評估

**✅ 優勢**
- **邏輯簡潔**：後端只需一次查詢返回月份全部預約，無需額外的時段映射處理
- **靈活性高**：前端可根據不同 status (0:撤回, 1:審核中, 2:通過, 3:拒絕) 自行判斷時段類型（已占用/待審核/已拒絕）
- **信息完整**：保留 bookingId、申請人、用途等其他業務信息，前端可實現更豐富的交互（如點擊查看詳情）
- **數據量可控**：一個月最多 31 天，假設每天平均 2-3 筆預約，月度最多 60-100 筆預約記錄，**網路傳輸量和資料庫查詢都完全可接受**

**⚠️ 潛在考量**
1. **API 回應體積**：每筆 BookingVO 包含 id、venueName、bookingDate、slots、status、createdAt 共 6 個欄位，100 筆預約約 10-15KB 的 JSON，仍在可接受範圍
2. **前端處理複雜度**：前端需要遍歷預約列表來構建時段占用狀態，但這是簡單的迴圈判邏輯，不會造成性能問題
3. **狀態定義明確**：確保前端清楚理解各 status 值的含義，特別是 status=1 (審核中) 時是否應計入「已占用」時段

### Steps

1. **擴展 VenueCalendarMonthVO** —— 在 [VenueCalendarMonthVO.java](file:///C:/Users/wl110/Desktop/venue-reservation-service/src/main/java/tw/edu/ncu/osa/venue_reservation_service/model/vo/VenueCalendarMonthVO.java) 中添加 `List<BookingVO> bookings` 欄位，存儲該月全部預約。

2. **新增 Mapper 查詢方法** —— 在 [BookingMapper.xml](file:///C:/Users/wl110/Desktop/venue-reservation-service/src/main/resources/mapper/BookingMapper.xml) 中新增方法 `selectBookingsByDateRangeForCalendar`，查詢指定場地月份範圍內的 **全部預約**（包括審核中、已通過、已拒絕等），並映射為 BookingVO。

3. **修改 getVenueCalendarMonth 邏輯** —— 在 [BookingServiceImpl.java](file:///C:/Users/wl110/Desktop/venue-reservation-service/src/main/java/tw/edu/ncu/osa/venue_reservation_service/service/impl/BookingServiceImpl.java) 第 277 行方法中：
    - 單次查詢月份全部預約（包括所有狀態）
    - 轉換為 BookingVO 清單
    - 賦值給 result.setBookings(bookingVOList)
    - 移除或簡化原有的 DaySimpleSummary 邏輯

4. **更新 DaySimpleSummary（可選保留）** —— 若前端需要快速判斷「該日是否有預約」的優化，可保留 `hasApprovedBooking` 和 `hasUserBooking` 布林欄，由後端在組裝時計算；或完全移除，由前端自行遍歷 bookings 清單判斷。

### Further Considerations

1. **status=1(審核中) 的時段處理** —— 前端在展示「已占用時段」時，是否應將審核中的預約計入？建議在文件中明確定義規則
2. **Mapper 映射策略** —— 需確認 BookingMapper 是否已有現成的 resultMap 或方法可直接轉換為 BookingVO，若無需新增自訂映射
3. **向後相容** —— 若現有前端已依賴舊版月曆 API（返回 days 摘要），需評估是否新增版本端點 `/calendar/month/v2` 或直接升級全量替換
