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
        // 1. 從 ThreadLocal 拿到 Mock 登入的用戶 ID
        String userId = UserContext.getUser().getUserId();

        // 2. 將前端傳來的 List<Integer> 轉為 24-bit 遮罩
        int requestMask = BookingUtils.convertToMask(request.getSlots());

        // 3. 衝突檢查：調用 Mapper 檢查資料庫是否有「已通過」且時段重疊的案件
        int conflicts = bookingMapper.countConflictingApprovedBookings(
                request.getVenueId(),
                request.getBookingDate(),
                requestMask
        );

        if (conflicts > 0) {
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
            booking.setContactInfo(objectMapper.writeValueAsString(request.getContactInfo()));
        } catch (JsonProcessingException e) {
            log.error("聯絡資訊轉換 JSON 失敗", e);
            throw new RuntimeException("資料格式錯誤");
        }

        bookingMapper.insertBooking(booking);

        // 5. 處理設備借用關聯 (如果有的話)
        if (request.getEquipmentIds() != null && !request.getEquipmentIds().isEmpty()) {
            for (Long equipId : request.getEquipmentIds()) {
                bookingMapper.insertBookingEquipment(booking.getId(), equipId);
            }
        }

        log.info("用戶 {} 成功建立預約申請 ID：{}", userId, booking.getId());
        return booking.getId();
    }

    // ==========================================
    // 2. 查詢預約
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public List<BookingVO> getMyBookings() {
        // 1. 從 ThreadLocal 獲取當前登入的用戶 ID
        String userId = UserContext.getUser().getUserId();

        // 2. 調用 Mapper 查詢該用戶所有預約申請
        List<Booking> bookings = bookingMapper.selectByUserId(userId);

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
        }

        return bookingVOList;
    }

    // ==========================================
    // 3. 修改預約
    // ==========================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBooking(Long bookingId, BookingRequestDTO request) {
        // 1. 從 ThreadLocal 獲取當前登入的用戶 ID
        String userId = UserContext.getUser().getUserId();

        // 2. 查詢原預約案是否存在
        Booking existingBooking = bookingMapper.selectById(bookingId);
        if (existingBooking == null) {
            throw new RuntimeException("預約案不存在");
        }

        // 3. 驗證該預約案是否屬於當前用戶
        if (!existingBooking.getUserId().equals(userId)) {
            throw new RuntimeException("無權限修改他人的預約申請");
        }

        // 4. 檢查預約狀態：僅限「審核中」或「已通過」的案件可修改
        if (existingBooking.getStatus() != 1 && existingBooking.getStatus() != 2) {
            throw new RuntimeException("該預約申請已被拒絕或已撤回，無法修改");
        }

        // 5. 進行時段衝突檢查（排除該預約案本身）
        int newRequestMask = BookingUtils.convertToMask(request.getSlots());
        int conflicts = bookingMapper.countConflictingApprovedBookings(
                request.getVenueId(),
                request.getBookingDate(),
                newRequestMask
        );
        if (conflicts > 0) {
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

        try {
            updatedBooking.setContactInfo(objectMapper.writeValueAsString(request.getContactInfo()));
        } catch (JsonProcessingException e) {
            log.error("聯絡資訊轉換 JSON 失敗", e);
            throw new RuntimeException("資料格式錯誤");
        }

        // 執行更新操作
        bookingMapper.updateBooking(updatedBooking);

        log.info("用戶 {} 成功修改預約申請 ID：{}", userId, bookingId);
    }

    // ==========================================
    // 4. 撤回預約
    // ==========================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawBooking(Long bookingId) {
        // 1. 從 ThreadLocal 獲取當前登入的用戶 ID
        String userId = UserContext.getUser().getUserId();

        // 2. 查詢預約案是否存在
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null) {
            throw new RuntimeException("預約案不存在");
        }

        // 3. 驗證該預約案是否屬於當前用戶
        if (!booking.getUserId().equals(userId)) {
            throw new RuntimeException("無權限撤回他人的預約申請");
        }

        // 4. 檢查預約狀態：僅限「審核中」或「已通過」的案件可撤回
        if (booking.getStatus() != 1 && booking.getStatus() != 2) {
            throw new RuntimeException("已拒絕或已撤回之申請無法再次撤回");
        }

        // 5. 使用樂觀鎖更新狀態為「撤回」(0)
        int result = bookingMapper.updateStatusWithVersion(bookingId, 0, booking.getVersion());
        if (result == 0) {
            throw new RuntimeException("版本號已過期，請重新加載數據");
        }

        log.info("用戶 {} 成功撤回預約申請 ID：{}", userId, bookingId);
    }
}