package tw.edu.ncu.osa.venue_reservation_service.service;

import tw.edu.ncu.osa.venue_reservation_service.model.dto.BookingRequestDTO;

public interface BookingService {
    /**
     * 建立場地預約申請
     * @param request 預約請求資料
     * @return 建立成功的申請案 ID
     */
    Long createBooking(BookingRequestDTO request);
}