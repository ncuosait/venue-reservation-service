package tw.edu.ncu.osa.venue_reservation_service.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tw.edu.ncu.osa.venue_reservation_service.mapper.BookingMapper;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.BookingRequestDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.Booking;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.BookingVO;
import tw.edu.ncu.osa.venue_reservation_service.service.BookingService;
import tw.edu.ncu.osa.venue_reservation_service.util.BookingUtils;
import tw.edu.ncu.osa.venue_reservation_service.util.UserContext;
import java.util.ArrayList;
import java.util.List;

/**
 * 預約服務實現類
 * 處理預約相關的業務邏輯，包括建立、查詢、修改和撤回預約申請
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingMapper bookingMapper;
    private final ObjectMapper objectMapper;

    // ==========================================
    // 1. 建立預約
    // ==========================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBooking(BookingRequestDTO request) {
        log.info("【BookingService】[createBooking] 開始建立預約申請");
        // 1. 從 ThreadLocal 拿到 Mock 登入的用戶 ID
        String userId = UserContext.getUser().getUserId();
        log.info("【BookingService】[createBooking] 獲取當前用戶ID={}", userId);

        // 2. 將前端傳來的 List<Integer> 轉為 24-bit 遮罩
        int requestMask = BookingUtils.convertToMask(request.getSlots());
        log.info("【BookingService】[createBooking] 將時段清單轉換為位元遮罩，slots={}，mask={}", 
                request.getSlots(), String.format("0x%06X", requestMask));

        // 3. 衝突檢查：調用 Mapper 檢查資料庫是否有「已通過」且時段重疊的案件
        log.info("【BookingService】[createBooking] 進行時段衝突檢查，venueId={}, bookingDate={}", 
                request.getVenueId(), request.getBookingDate());
        int conflicts = bookingMapper.countConflictingApprovedBookings(
                request.getVenueId(),
                request.getBookingDate(),
                requestMask
        );
        log.info("【BookingService】[createBooking] 衝突檢查結果：{}筆衝突預約", conflicts);

        if (conflicts > 0) {
            log.warn("【BookingService】[createBooking] 時段已被佔用，無法建立預約");
            throw new RuntimeException("該時段已被其他已通過之申請佔用");
        }

        // 4. 建構 Booking 實體並存檔
        Booking booking = new Booking();
        booking.setVenueId(request.getVenueId());
        booking.setUserId(userId);
        booking.setBookingDate(request.getBookingDate());
        booking.setTimeSlots(requestMask);
        booking.setStatus(1); // 初始狀態：審核中
        booking.setPurpose(request.getPurpose());
        booking.setPCount(request.getParticipantCount());

        try {
            // 將聯絡人物件轉為 JSON 字串存入資料庫
            String contactInfoJson = objectMapper.writeValueAsString(request.getContactInfo());
            booking.setContactInfo(contactInfoJson);
            log.debug("【BookingService】[createBooking] 聯絡資訊已轉換為JSON：{}", contactInfoJson);
        } catch (JsonProcessingException e) {
            log.error("【BookingService】[createBooking] 聯絡資訊轉換 JSON 失敗", e);
            throw new RuntimeException("資料格式錯誤");
        }

        log.debug("【BookingService】[createBooking] 預約對象詳細信息 - venueId={}, date={}, slots={}, purpose={}, 參與人數={}, 聯絡人={}", 
                booking.getVenueId(), booking.getBookingDate(), request.getSlots(), 
                booking.getPurpose(), booking.getPCount(), request.getContactInfo().getName());

        bookingMapper.insertBooking(booking);
        log.info("【BookingService】[createBooking] 成功插入預約記錄到數據庫，新預約ID={}", booking.getId());

        // 5. 處理設備借用關聯 (如果有的話)
        if (request.getEquipmentIds() != null && !request.getEquipmentIds().isEmpty()) {
            log.info("【BookingService】[createBooking] 開始關聯設備，共 {} 個設備", request.getEquipmentIds().size());
            for (Long equipId : request.getEquipmentIds()) {
                bookingMapper.insertBookingEquipment(booking.getId(), equipId);
                log.debug("【BookingService】[createBooking] 已關聯設備，equipId={}", equipId);
            }
            log.info("【BookingService】[createBooking] 所有設備關聯完成");
        }

        log.info("【BookingService】[createBooking] 用戶 {} 成功建立預約申請 ID：{}", userId, booking.getId());
        return booking.getId();
    }

    // ==========================================
    // 2. 查詢預約
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public List<BookingVO> getMyBookings() {
        log.info("【BookingService】[getMyBookings] 開始查詢個人預約清單");
        // 1. 從 ThreadLocal 獲取當前登入的用戶 ID
        String userId = UserContext.getUser().getUserId();
        log.info("【BookingService】[getMyBookings] 當前用戶ID={}", userId);

        // 2. 調用 Mapper 查詢該用戶所有預約申請
        List<Booking> bookings = bookingMapper.selectByUserId(userId);
        log.info("【BookingService】[getMyBookings] 從數據庫查詢到 {} 筆預約記錄，userId={}", bookings.size(), userId);
        log.debug("【BookingService】[getMyBookings] 原始預約數據：{}", bookings);

        // 3. 將 Booking 實體轉換為 BookingVO
        List<BookingVO> bookingVOList = new ArrayList<>();
        for (Booking booking : bookings) {
            BookingVO vo = new BookingVO();
            vo.setId(booking.getId());
            // TODO: 此處需透過 venue_id 查詢場地名稱，建議在 SQL 層透過 LEFT JOIN 實現
            vo.setVenueName("場地 " + booking.getVenueId());
            vo.setBookingDate(booking.getBookingDate());
            // 將 24-bit 位元遮罩轉換回時段清單
            vo.setSlots(BookingUtils.parseMaskToList(booking.getTimeSlots()));
            vo.setStatus(booking.getStatus());
            vo.setCreatedAt(booking.getCreatedAt());
            bookingVOList.add(vo);
            log.debug("【BookingService】[getMyBookings] 轉換預約VO - bookingId={}, venueId={}, date={}, slots={}, status={}", 
                    vo.getId(), booking.getVenueId(), vo.getBookingDate(), vo.getSlots(), vo.getStatus());
        }

        log.info("【BookingService】[getMyBookings] 成功轉換 {} 筆預約VO，準備返回", bookingVOList.size());
        return bookingVOList;
    }

    // ==========================================
    // 3. 修改預約
    // ==========================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBooking(Long bookingId, BookingRequestDTO request) {
        log.info("【BookingService】[updateBooking] 開始修改預約申請，bookingId={}", bookingId);
        // 1. 從 ThreadLocal 獲取當前登入的用戶 ID
        String userId = UserContext.getUser().getUserId();
        log.info("【BookingService】[updateBooking] 當前用戶ID={}", userId);

        // 2. 查詢原預約案是否存在
        Booking existingBooking = bookingMapper.selectById(bookingId);
        log.debug("【BookingService】[updateBooking] 查詢原預約記錄結果：{}", existingBooking);
        
        if (existingBooking == null) {
            log.error("【BookingService】[updateBooking] 預約案不存在，bookingId={}", bookingId);
            throw new RuntimeException("預約案不存在");
        }

        // 3. 驗證該預約案是否屬於當前用戶
        if (!existingBooking.getUserId().equals(userId)) {
            log.warn("【BookingService】[updateBooking] 用戶嘗試修改他人的預約，userId={}，bookingId={}, 預約所有者={}", 
                    userId, bookingId, existingBooking.getUserId());
            throw new RuntimeException("無權限修改他人的預約申請");
        }

        // 4. 檢查預約狀態：僅限「審核中」或「已通過」的案件可修改
        log.info("【BookingService】[updateBooking] 檢查預約狀態，bookingId={}, currentStatus={}", bookingId, existingBooking.getStatus());
        if (existingBooking.getStatus() != 1 && existingBooking.getStatus() != 2) {
            log.warn("【BookingService】[updateBooking] 預約狀態不符合修改條件，bookingId={}, status={}", bookingId, existingBooking.getStatus());
            throw new RuntimeException("該預約申請已被拒絕或已撤回，無法修改");
        }

        // 5. 進行時段衝突檢查（排除該預約案本身）
        int newRequestMask = BookingUtils.convertToMask(request.getSlots());
        log.info("【BookingService】[updateBooking] 進行修改後時段衝突檢查，新時段slots={}，mask={}", 
                request.getSlots(), String.format("0x%06X", newRequestMask));
        int conflicts = bookingMapper.countConflictingApprovedBookings(
                request.getVenueId(),
                request.getBookingDate(),
                newRequestMask
        );
        log.info("【BookingService】[updateBooking] 衝突檢查結果：{}筆衝突預約", conflicts);
        
        if (conflicts > 0) {
            log.warn("【BookingService】[updateBooking] 修改後的時段已被佔用，bookingId={}", bookingId);
            throw new RuntimeException("修改後的時段已被其他已通過之申請佔用");
        }

        // 6. 更新預約資訊並重置狀態為「審核中」
        Booking updatedBooking = new Booking();
        updatedBooking.setId(bookingId);
        updatedBooking.setVenueId(request.getVenueId());
        updatedBooking.setBookingDate(request.getBookingDate());
        updatedBooking.setTimeSlots(newRequestMask);
        updatedBooking.setStatus(1); // 重置為審核中
        updatedBooking.setPurpose(request.getPurpose());
        updatedBooking.setPCount(request.getParticipantCount());

        log.debug("【BookingService】[updateBooking] 修改前 - venueId={}, date={}, slots={}, status={}", 
                existingBooking.getVenueId(), existingBooking.getBookingDate(), 
                BookingUtils.parseMaskToList(existingBooking.getTimeSlots()), existingBooking.getStatus());
        log.debug("【BookingService】[updateBooking] 修改後 - venueId={}, date={}, slots={}, status={}", 
                updatedBooking.getVenueId(), updatedBooking.getBookingDate(), 
                request.getSlots(), updatedBooking.getStatus());

        try {
            updatedBooking.setContactInfo(objectMapper.writeValueAsString(request.getContactInfo()));
        } catch (JsonProcessingException e) {
            log.error("【BookingService】[updateBooking] 聯絡資訊轉換 JSON 失敗", e);
            throw new RuntimeException("資料格式錯誤");
        }

        // 執行更新操作
        bookingMapper.updateBooking(updatedBooking);
        log.info("【BookingService】[updateBooking] 用戶 {} 成功修改預約申請 ID：{}", userId, bookingId);
    }

    // ==========================================
    // 4. 撤回預約
    // ==========================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawBooking(Long bookingId) {
        log.info("【BookingService】[withdrawBooking] 開始撤回預約申請，bookingId={}", bookingId);
        // 1. 從 ThreadLocal 獲取當前登入的用戶 ID
        String userId = UserContext.getUser().getUserId();
        log.info("【BookingService】[withdrawBooking] 當前用戶ID={}", userId);

        // 2. 查詢預約案是否存在
        Booking booking = bookingMapper.selectById(bookingId);
        log.debug("【BookingService】[withdrawBooking] 查詢預約記錄結果：{}", booking);
        
        if (booking == null) {
            log.error("【BookingService】[withdrawBooking] 預約案不存在，bookingId={}", bookingId);
            throw new RuntimeException("預約案不存在");
        }

        // 3. 驗證該預約案是否屬於當前用戶
        if (!booking.getUserId().equals(userId)) {
            log.warn("【BookingService】[withdrawBooking] 用戶嘗試撤回他人的預約，userId={}，bookingId={}, 預約所有者={}", 
                    userId, bookingId, booking.getUserId());
            throw new RuntimeException("無權限撤回他人的預約申請");
        }

        // 4. 檢查預約狀態：僅限「審核中」或「已通過」的案件可撤回
        log.info("【BookingService】[withdrawBooking] 檢查預約狀態，bookingId={}, currentStatus={}", bookingId, booking.getStatus());
        if (booking.getStatus() != 1 && booking.getStatus() != 2) {
            log.warn("【BookingService】[withdrawBooking] 預約狀態不符合撤回條件，bookingId={}, status={}", bookingId, booking.getStatus());
            throw new RuntimeException("已拒絕或已撤回之申請無法再次撤回");
        }

        // 5. 使用樂觀鎖更新狀態為「撤回」(0)
        log.info("【BookingService】[withdrawBooking] 準備使用樂觀鎖更新狀態為撤回(0)，bookingId={}, version={}", 
                bookingId, booking.getVersion());
        int result = bookingMapper.updateStatusWithVersion(bookingId, 0, booking.getVersion());
        
        if (result == 0) {
            log.error("【BookingService】[withdrawBooking] 樂觀鎖更新失敗，版本號已過期，bookingId={}, version={}", 
                    bookingId, booking.getVersion());
            throw new RuntimeException("版本號已過期，請重新加載數據");
        }

        log.info("【BookingService】[withdrawBooking] 用戶 {} 成功撤回預約申請 ID：{}", userId, bookingId);
    }

    // ==========================================
    // 5. 日曆視圖查詢
    // ==========================================


    @Override
    @Transactional(readOnly = true)
    public tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarMonthVO getVenueCalendarMonth(
            Long venueId,
            Integer year,
            Integer month) {

        log.info("【BookingService】[getVenueCalendarMonth] 開始查詢場地月曆視圖，venueId={}, year={}, month={}", 
                venueId, year, month);

        // ==========================================
        // 1. 參數驗證
        // ==========================================
        if (venueId == null || venueId <= 0) {
            log.error("【BookingService】[getVenueCalendarMonth] 場地ID無效，venueId={}", venueId);
            throw new IllegalArgumentException("場地 ID 不可為空或為負數");
        }

        if (year == null || month == null || month < 1 || month > 12) {
            log.error("【BookingService】[getVenueCalendarMonth] 年月份格式不正確，year={}, month={}", year, month);
            throw new IllegalArgumentException("年份或月份格式不正確");
        }

        // ==========================================
        // 2. 計算月份日期範圍
        // ==========================================
        java.time.YearMonth yearMonth = java.time.YearMonth.of(year, month);
        java.time.LocalDate startDate = yearMonth.atDay(1);
        java.time.LocalDate endDate = yearMonth.atEndOfMonth();
        log.info("【BookingService】[getVenueCalendarMonth] 計算月份範圍，startDate={}, endDate={}", startDate, endDate);

        // ==========================================
        // 3. 查詢該月份的全部預約（包括所有狀態）
        // ==========================================
        List<tw.edu.ncu.osa.venue_reservation_service.model.vo.BookingVO> bookingVOList = 
                bookingMapper.selectBookingsByDateRangeForCalendar(venueId, startDate, endDate);
        log.info("【BookingService】[getVenueCalendarMonth] 查詢到 {} 筆預約記錄", bookingVOList.size());
        log.debug("【BookingService】[getVenueCalendarMonth] 預約詳情：{}", bookingVOList);

        // ==========================================
        // 4. 構建日期對應的預約映射表（用於快速判斷）
        // ==========================================
        java.util.Map<java.time.LocalDate, Boolean> approvedMap = new java.util.HashMap<>();
        for (tw.edu.ncu.osa.venue_reservation_service.model.vo.BookingVO booking : bookingVOList) {
            if (booking.getStatus() == 2) { // status=2 表示已通過
                approvedMap.put(booking.getBookingDate(), true);
                log.debug("【BookingService】[getVenueCalendarMonth] 標記已通過預約日期：{}", booking.getBookingDate());
            }
        }

        // ==========================================
        // 5. 組裝月視圖 VO
        // ==========================================
        tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarMonthVO result =
                new tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarMonthVO();
        result.setYear(year);
        result.setMonth(month);

        // 組裝日期摘要（保留舊有欄位以保持相容性）
        List<tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarMonthVO.DaySimpleSummary> days =
                new ArrayList<>();

        java.time.LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarMonthVO.DaySimpleSummary daySummary =
                    new tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarMonthVO.DaySimpleSummary();

            daySummary.setDate(currentDate.toString());
            daySummary.setHasApprovedBooking(approvedMap.getOrDefault(currentDate, false));
            // hasUserBooking 可由前端根據登入用戶 ID 判斷，此處簡化為 false
            daySummary.setHasUserBooking(false);

            days.add(daySummary);
            currentDate = currentDate.plusDays(1);
        }

        result.setDays(days);
        
        // 設置月份的全部預約列表
        result.setBookings(bookingVOList);

        log.info("【BookingService】[getVenueCalendarMonth] 成功組裝月曆視圖，共 {} 天的摘要、{} 筆預約記錄", 
                days.size(), bookingVOList.size());
        log.debug("【BookingService】[getVenueCalendarMonth] 返回月曆VO：{}", result);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarWeekVO getVenueCalendarWeek(
            Long venueId,
            java.time.LocalDate weekStartDate) {

        log.info("【BookingService】[getVenueCalendarWeek] 開始查詢場地周曆視圖，venueId={}, weekStart={}", 
                venueId, weekStartDate);

        // ==========================================
        // 1. 參數驗證
        // ==========================================
        if (venueId == null || venueId <= 0) {
            log.error("【BookingService】[getVenueCalendarWeek] 場地ID無效，venueId={}", venueId);
            throw new IllegalArgumentException("場地 ID 不可為空或為負數");
        }

        if (weekStartDate == null) {
            log.error("【BookingService】[getVenueCalendarWeek] 周開始日期為空");
            throw new IllegalArgumentException("周開始日期不可為空");
        }

        // 驗證周開始日期必須為周一
        if (weekStartDate.getDayOfWeek() != java.time.DayOfWeek.MONDAY) {
            log.error("【BookingService】[getVenueCalendarWeek] 周開始日期非周一，weekStart={}, dayOfWeek={}", 
                    weekStartDate, weekStartDate.getDayOfWeek());
            throw new IllegalArgumentException("周開始日期必須為周一");
        }

        // ==========================================
        // 2. 計算周的日期範圍
        // ==========================================
        java.time.LocalDate weekEndDate = weekStartDate.plusDays(6);
        log.info("【BookingService】[getVenueCalendarWeek] 計算周日期範圍，weekStart={}, weekEnd={}", 
                weekStartDate, weekEndDate);

        // ==========================================
        // 3. 查詢該周的已通過預約和用戶預約
        // ==========================================
        String userId = UserContext.getUser().getUserId();
        log.info("【BookingService】[getVenueCalendarWeek] 當前用戶ID={}", userId);
        
        List<Booking> approvedBookings = bookingMapper.selectApprovedBookingsByDateRange(
                venueId, weekStartDate, weekEndDate);
        log.info("【BookingService】[getVenueCalendarWeek] 查詢到 {} 筆已通過預約", approvedBookings.size());
        log.debug("【BookingService】[getVenueCalendarWeek] 已通過預約詳情：{}", approvedBookings);
        
        List<Booking> userBookings = bookingMapper.selectUserBookingsByDateRange(
                userId, venueId, weekStartDate, weekEndDate);
        log.info("【BookingService】[getVenueCalendarWeek] 查詢到 {} 筆用戶預約", userBookings.size());
        log.debug("【BookingService】[getVenueCalendarWeek] 用戶預約詳情：{}", userBookings);

        // ==========================================
        // 4. 按日期分組預約
        // ==========================================
        java.util.Map<java.time.LocalDate, java.util.List<Booking>> approvedByDate =
                new java.util.HashMap<>();
        for (Booking booking : approvedBookings) {
            java.time.LocalDate date = booking.getBookingDate();
            approvedByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(booking);
            log.debug("【BookingService】[getVenueCalendarWeek] 分組已通過預約 - date={}, bookingId={}", date, booking.getId());
        }

        java.util.Map<java.time.LocalDate, java.util.List<Booking>> userByDate =
                new java.util.HashMap<>();
        for (Booking booking : userBookings) {
            java.time.LocalDate date = booking.getBookingDate();
            userByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(booking);
            log.debug("【BookingService】[getVenueCalendarWeek] 分組用戶預約 - date={}, bookingId={}", date, booking.getId());
        }

        // ==========================================
        // 5. 組裝周視圖 VO
        // ==========================================
        tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarWeekVO result =
                new tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarWeekVO();
        result.setWeekStart(weekStartDate.toString());
        result.setWeekEnd(weekEndDate.toString());

        List<tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarWeekVO.DayDetailSummary> days =
                new ArrayList<>();

        java.time.LocalDate currentDate = weekStartDate;
        while (!currentDate.isAfter(weekEndDate)) {
            tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarWeekVO.DayDetailSummary dayDetail =
                    new tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarWeekVO.DayDetailSummary();

            dayDetail.setDate(currentDate.toString());
            dayDetail.setDayOfWeek(getDayOfWeekChinese(currentDate));

            // 合併該日的已通過時段
            java.util.Set<Integer> approvedSlots = new java.util.HashSet<>();
            java.util.List<Booking> approvedForDate = approvedByDate.getOrDefault(currentDate, new ArrayList<>());
            for (Booking booking : approvedForDate) {
                List<Integer> slots = BookingUtils.parseMaskToList(booking.getTimeSlots());
                approvedSlots.addAll(slots);
                log.debug("【BookingService】[getVenueCalendarWeek] 合併已通過時段 - date={}, slots={}", currentDate, slots);
            }
            List<Integer> approvedSlotsList = new ArrayList<>(approvedSlots);
            java.util.Collections.sort(approvedSlotsList);
            dayDetail.setApprovedSlots(approvedSlotsList);

            // 合併該日的用戶時段
            java.util.Set<Integer> userSlots = new java.util.HashSet<>();
            java.util.List<Booking> userForDate = userByDate.getOrDefault(currentDate, new ArrayList<>());
            for (Booking booking : userForDate) {
                List<Integer> slots = BookingUtils.parseMaskToList(booking.getTimeSlots());
                userSlots.addAll(slots);
                log.debug("【BookingService】[getVenueCalendarWeek] 合併用戶時段 - date={}, slots={}", currentDate, slots);
            }
            List<Integer> userSlotsList = new ArrayList<>(userSlots);
            java.util.Collections.sort(userSlotsList);
            dayDetail.setUserSlots(userSlotsList);

            log.debug("【BookingService】[getVenueCalendarWeek] 組裝日期詳情 - date={}, dayOfWeek={}, approvedSlots={}, userSlots={}", 
                    currentDate, dayDetail.getDayOfWeek(), approvedSlotsList, userSlotsList);

            days.add(dayDetail);
            currentDate = currentDate.plusDays(1);
        }

        result.setDays(days);
        log.info("【BookingService】[getVenueCalendarWeek] 成功組裝周曆視圖，共 {} 天的數據", days.size());
        log.debug("【BookingService】[getVenueCalendarWeek] 返回周曆VO：{}", result);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarDayVO getVenueCalendarDay(
            Long venueId,
            java.time.LocalDate date) {

        log.info("【BookingService】[getVenueCalendarDay] 開始查詢場地日曆視圖，venueId={}, date={}", venueId, date);

        // ==========================================
        // 1. 參數驗證
        // ==========================================
        if (venueId == null || venueId <= 0) {
            log.error("【BookingService】[getVenueCalendarDay] 場地ID無效，venueId={}", venueId);
            throw new IllegalArgumentException("場地 ID 不可為空或為負數");
        }

        if (date == null) {
            log.error("【BookingService】[getVenueCalendarDay] 日期為空");
            throw new IllegalArgumentException("日期不可為空");
        }

        // ==========================================
        // 2. 查詢該日的已通過預約和用戶預約
        // ==========================================
        String userId = UserContext.getUser().getUserId();
        log.info("【BookingService】[getVenueCalendarDay] 當前用戶ID={}", userId);
        
        List<Booking> approvedBookings = bookingMapper.selectApprovedBookingsByDateRange(
                venueId, date, date);
        log.info("【BookingService】[getVenueCalendarDay] 查詢到 {} 筆已通過預約", approvedBookings.size());
        log.debug("【BookingService】[getVenueCalendarDay] 已通過預約詳情：{}", approvedBookings);
        
        List<Booking> userBookings = bookingMapper.selectUserBookingsByDateRange(
                userId, venueId, date, date);
        log.info("【BookingService】[getVenueCalendarDay] 查詢到 {} 筆用戶預約", userBookings.size());
        log.debug("【BookingService】[getVenueCalendarDay] 用戶預約詳情：{}", userBookings);

        // ==========================================
        // 3. 合併已通過時段（去重）
        // ==========================================
        java.util.Set<Integer> approvedSlotsSet = new java.util.HashSet<>();
        for (Booking booking : approvedBookings) {
            List<Integer> slots = BookingUtils.parseMaskToList(booking.getTimeSlots());
            approvedSlotsSet.addAll(slots);
            log.debug("【BookingService】[getVenueCalendarDay] 已通過時段合併 - bookingId={}, slots={}", booking.getId(), slots);
        }
        List<Integer> approvedSlots = new ArrayList<>(approvedSlotsSet);
        java.util.Collections.sort(approvedSlots);
        log.info("【BookingService】[getVenueCalendarDay] 已通過時段去重後：{}", approvedSlots);

        // ==========================================
        // 4. 合併用戶時段（去重）
        // ==========================================
        java.util.Set<Integer> userSlotsSet = new java.util.HashSet<>();
        for (Booking booking : userBookings) {
            List<Integer> slots = BookingUtils.parseMaskToList(booking.getTimeSlots());
            userSlotsSet.addAll(slots);
            log.debug("【BookingService】[getVenueCalendarDay] 用戶時段合併 - bookingId={}, slots={}", booking.getId(), slots);
        }
        List<Integer> userSlots = new ArrayList<>(userSlotsSet);
        java.util.Collections.sort(userSlots);
        log.info("【BookingService】[getVenueCalendarDay] 用戶時段去重後：{}", userSlots);

        // ==========================================
        // 5. 組裝用戶預約詳情列表
        // ==========================================
        List<tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarDayVO.UserBookingDetail> userDetails =
                new ArrayList<>();
        for (Booking booking : userBookings) {
            tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarDayVO.UserBookingDetail detail =
                    new tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarDayVO.UserBookingDetail();

            detail.setBookingId(booking.getId());
            detail.setSlots(BookingUtils.parseMaskToList(booking.getTimeSlots()));
            detail.setStatus(booking.getStatus());
            detail.setPurpose(booking.getPurpose());
            detail.setCreatedAt(booking.getCreatedAt());

            userDetails.add(detail);
            log.debug("【BookingService】[getVenueCalendarDay] 組裝用戶預約詳情 - bookingId={}, slots={}, status={}, purpose={}", 
                    detail.getBookingId(), detail.getSlots(), detail.getStatus(), detail.getPurpose());
        }
        log.info("【BookingService】[getVenueCalendarDay] 用戶預約詳情列表，共 {} 筆", userDetails.size());

        // ==========================================
        // 6. 組裝日視圖 VO
        // ==========================================
        tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarDayVO result =
                new tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarDayVO();

        result.setVenueId(venueId);
        result.setVenueName("場地 " + venueId); // TODO: 從場地表查詢實際場地名稱
        result.setDate(date.toString());
        result.setDayOfWeek(getDayOfWeekChinese(date));
        result.setApprovedSlots(approvedSlots);
        result.setUserSlots(userSlots);
        result.setUserBookingDetails(userDetails);

        log.info("【BookingService】[getVenueCalendarDay] 成功組裝日曆視圖，venueId={}, date={}, dayOfWeek={}", 
                venueId, date, result.getDayOfWeek());
        log.debug("【BookingService】[getVenueCalendarDay] 返回日曆VO：{}", result);
        
        return result;
    }

    // ==========================================
    // 輔助方法
    // ==========================================

    /**
     * 將 LocalDate 轉換為中文星期幾
     * @param date 日期
     * @return 中文星期幾，例如「星期一」
     */
    private String getDayOfWeekChinese(java.time.LocalDate date) {
        switch (date.getDayOfWeek()) {
            case MONDAY:
                return "星期一";
            case TUESDAY:
                return "星期二";
            case WEDNESDAY:
                return "星期三";
            case THURSDAY:
                return "星期四";
            case FRIDAY:
                return "星期五";
            case SATURDAY:
                return "星期六";
            case SUNDAY:
                return "星期日";
            default:
                return "未知";
        }
    }
}

