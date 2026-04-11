package tw.edu.ncu.osa.venue_reservation_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import tw.edu.ncu.osa.venue_reservation_service.common.result.Result;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.BookingRequestDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.BookingVO;
import tw.edu.ncu.osa.venue_reservation_service.service.BookingService;
import java.util.List;

/**
 * 預約管理 API 控制器
 * 負責處理預約相關的 HTTP 請求
 */
@Slf4j
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // ==========================================
    // 1. 提交預約申請
    // ==========================================

    /**
     * 提交預約申請
     * @param request 預約申請資料
     * @return 新建立的預約案 ID
     */
    @PostMapping
    public Result<Long> createBooking(@Valid @RequestBody BookingRequestDTO request) {
        log.info("【BookingController】收到請求：提交預約申請");
        log.info("【BookingController】請求參數 - venueId={}, bookingDate={}, slots={}, purpose={}",
                request.getVenueId(), request.getBookingDate(), request.getSlots(), request.getPurpose());
        try {
            Long bookingId = bookingService.createBooking(request);
            log.info("【BookingController】成功建立預約申請，新預約 ID={}", bookingId);
            return Result.success(bookingId);
        } catch (Exception e) {
            log.error("【BookingController】提交預約申請失敗，venueId={}, bookingDate={}", 
                    request.getVenueId(), request.getBookingDate(), e);
            throw e;
        }
    }

    // ==========================================
    // 2. 查詢預約
    // ==========================================

    /**
     * 查看個人預約清單
     * @return 當前登入用戶的預約申請列表
     */
    @GetMapping("/my")
    public Result<List<BookingVO>> getMyBookings() {
        log.info("【BookingController】收到請求：查看個人預約清單");
        try {
            List<BookingVO> bookings = bookingService.getMyBookings();
            log.info("【BookingController】成功查詢個人預約清單，共 {} 筆預約", bookings.size());
            log.debug("【BookingController】返回預約數據：{}", bookings);
            return Result.success(bookings);
        } catch (Exception e) {
            log.error("【BookingController】查詢個人預約清單失敗", e);
            throw e;
        }
    }

    // ==========================================
    // 3. 修改預約
    // ==========================================

    /**
     * 修改預約申請
     * @param bookingId 預約案 ID
     * @param request 修改後的預約資料
     * @return 操作結果
     */
    @PutMapping("/{id}")
    public Result<Void> updateBooking(
            @PathVariable(name = "id") Long bookingId,
            @Valid @RequestBody BookingRequestDTO request) {
        log.info("【BookingController】收到請求：修改預約申請，bookingId={}", bookingId);
        log.info("【BookingController】修改參數 - venueId={}, bookingDate={}, slots={}", 
                request.getVenueId(), request.getBookingDate(), request.getSlots());
        try {
            bookingService.updateBooking(bookingId, request);
            log.info("【BookingController】成功修改預約申請，bookingId={}", bookingId);
            return Result.success(null);
        } catch (Exception e) {
            log.error("【BookingController】修改預約申請失敗，bookingId={}", bookingId, e);
            throw e;
        }
    }

    // ==========================================
    // 4. 撤回預約
    // ==========================================

    /**
     * 撤回預約申請
     * @param bookingId 預約案 ID
     * @return 操作結果
     */
    @PutMapping("/{id}/withdraw")
    public Result<Void> withdrawBooking(@PathVariable(name = "id") Long bookingId) {
        log.info("【BookingController】收到請求：撤回預約申請，bookingId={}", bookingId);
        try {
            bookingService.withdrawBooking(bookingId);
            log.info("【BookingController】成功撤回預約申請，bookingId={}", bookingId);
            return Result.success(null);
        } catch (Exception e) {
            log.error("【BookingController】撤回預約申請失敗，bookingId={}", bookingId, e);
            throw e;
        }
    }

    // ==========================================
    // 5. 日曆視圖查詢
    // ==========================================

    /**
     * 獲取場地月曆視圖
     * 顯示該月每日是否有已占用時段和用戶預約
     * @param venueId 場地 ID
     * @param year 年份
     * @param month 月份 (1-12)
     * @return 月曆視圖資料
     */
    @GetMapping("/calendar/month")
    public Result<tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarMonthVO> getCalendarMonth(
            @RequestParam Long venueId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        log.info("【BookingController】收到請求：獲取場地月曆視圖，venueId={}, year={}, month={}", 
                venueId, year, month);
        try {
            tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarMonthVO result =
                    bookingService.getVenueCalendarMonth(venueId, year, month);
            log.info("【BookingController】成功獲取月曆視圖，venueId={}, 共 {} 天數據", venueId, result.getDays().size());
            log.debug("【BookingController】返回月曆數據：{}", result);
            return Result.success(result);
        } catch (Exception e) {
            log.error("【BookingController】獲取月曆視圖失敗，venueId={}, year={}, month={}", 
                    venueId, year, month, e);
            throw e;
        }
    }

    /**
     * 獲取場地周曆視圖
     * 顯示該周每日的詳細時段占用情況
     * @param venueId 場地 ID
     * @param date 周開始日期 (必須為周一，ISO 8601 格式)
     * @return 周曆視圖資料
     */
    @GetMapping("/calendar/week")
    public Result<tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarWeekVO> getCalendarWeek(
            @RequestParam Long venueId,
            @RequestParam java.time.LocalDate date) {
        log.info("【BookingController】收到請求：獲取場地周曆視圖，venueId={}, weekStart={}", venueId, date);
        try {
            tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarWeekVO result =
                    bookingService.getVenueCalendarWeek(venueId, date);
            log.info("【BookingController】成功獲取周曆視圖，venueId={}, weekStart={}, 共 {} 天數據", 
                    venueId, date, result.getDays().size());
            log.debug("【BookingController】返回周曆數據：{}", result);
            return Result.success(result);
        } catch (Exception e) {
            log.error("【BookingController】獲取周曆視圖失敗，venueId={}, weekStart={}", venueId, date, e);
            throw e;
        }
    }

    /**
     * 獲取場地日曆視圖
     * 顯示該日的詳細時段占用情況和用戶預約列表
     * @param venueId 場地 ID
     * @param date 查詢日期 (ISO 8601 格式)
     * @return 日曆視圖資料
     */
    @GetMapping("/calendar/day")
    public Result<tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarDayVO> getCalendarDay(
            @RequestParam Long venueId,
            @RequestParam java.time.LocalDate date) {
        log.info("【BookingController】收到請求：獲取場地日曆視圖，venueId={}, date={}", venueId, date);
        try {
            tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarDayVO result =
                    bookingService.getVenueCalendarDay(venueId, date);
            log.info("【BookingController】成功獲取日曆視圖，venueId={}, date={}", venueId, date);
            log.debug("【BookingController】返回日曆數據：{}", result);
            return Result.success(result);
        } catch (Exception e) {
            log.error("【BookingController】獲取日曆視圖失敗，venueId={}, date={}", venueId, date, e);
            throw e;
        }
    }
}