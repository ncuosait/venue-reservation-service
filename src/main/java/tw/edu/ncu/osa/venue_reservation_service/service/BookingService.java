package tw.edu.ncu.osa.venue_reservation_service.service;

import tw.edu.ncu.osa.venue_reservation_service.model.dto.BookingRequestDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.BookingVO;
import java.util.List;

/**
 * 預約業務服務介面
 * 定義預約相關的業務操作契約
 */
public interface BookingService {
    
    // ==========================================
    // 建立預約
    // ==========================================
    
    /**
     * 建立場地預約申請
     * 檢查時段衝突並初始化預約案
     * @param request 預約請求資料
     * @return 建立成功的申請案 ID
     * @throws RuntimeException 當時段已被其他通過的申請佔用時拋出
     */
    Long createBooking(BookingRequestDTO request);

    // ==========================================
    // 查詢預約
    // ==========================================
    
    /**
     * 獲取當前登入用戶的所有預約申請清單
     * @return 預約申請列表 (按建立時間倒序)
     */
    List<BookingVO> getMyBookings();

    // ==========================================
    // 修改預約
    // ==========================================
    
    /**
     * 修改預約申請資訊
     * 僅限尚未被核准或已核准的案件可修改，修改後狀態重置為「審核中」
     * @param bookingId 預約案 ID
     * @param request 修改後的預約資料
     * @throws RuntimeException 當預約不存在或狀態不符合修改條件時拋出
     */
    void updateBooking(Long bookingId, BookingRequestDTO request);

    // ==========================================
    // 撤回預約
    // ==========================================
    
    /**
     * 撤回預約申請
     * 僅限狀態為「審核中」或「已通過」的案件可撤回
     * @param bookingId 預約案 ID
     * @throws RuntimeException 當預約不存在、不屬於當前用戶或狀態不符合撤回條件時拋出
     */
    void withdrawBooking(Long bookingId);

    // ==========================================
    // 日曆視圖查詢
    // ==========================================
    
    /**
     * 獲取場地月曆視圖
     * 顯示該月每日是否有已占用時段和用戶預約
     * @param venueId 場地 ID
     * @param year 年份
     * @param month 月份 (1-12)
     * @return 月曆視圖資料
     * @throws IllegalArgumentException 當參數無效時拋出
     */
    tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarMonthVO getVenueCalendarMonth(
            Long venueId, 
            Integer year, 
            Integer month
    );
    
    /**
     * 獲取場地周曆視圖
     * 顯示該周每日的詳細時段占用情況
     * @param venueId 場地 ID
     * @param weekStartDate 周開始日期 (必須為周一)
     * @return 周曆視圖資料
     * @throws IllegalArgumentException 當周開始日期不是周一或參數無效時拋出
     */
    tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarWeekVO getVenueCalendarWeek(
            Long venueId, 
            java.time.LocalDate weekStartDate
    );
    
    /**
     * 獲取場地日曆視圖
     * 顯示該日的詳細時段占用情況和用戶預約列表
     * @param venueId 場地 ID
     * @param date 查詢日期
     * @return 日曆視圖資料
     * @throws IllegalArgumentException 當參數無效時拋出
     */
    tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarDayVO getVenueCalendarDay(
            Long venueId, 
            java.time.LocalDate date
    );
}