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
import tw.edu.ncu.osa.venue_reservation_service.service.BookingService;
import tw.edu.ncu.osa.venue_reservation_service.util.BookingUtils;
import tw.edu.ncu.osa.venue_reservation_service.util.UserContext;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingMapper bookingMapper;
    private final ObjectMapper objectMapper;

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

        return booking.getId();
    }
}