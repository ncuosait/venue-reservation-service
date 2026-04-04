package tw.edu.ncu.osa.venue_reservation_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
        Long bookingId = bookingService.createBooking(request);
        return Result.success(bookingId);
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
        List<BookingVO> bookings = bookingService.getMyBookings();
        return Result.success(bookings);
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
        bookingService.updateBooking(bookingId, request);
        return Result.success(null);
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
        bookingService.withdrawBooking(bookingId);
        return Result.success(null);
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
        tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarMonthVO result =
                bookingService.getVenueCalendarMonth(venueId, year, month);
        return Result.success(result);
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
        tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarWeekVO result =
                bookingService.getVenueCalendarWeek(venueId, date);
        return Result.success(result);
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
        tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarDayVO result =
                bookingService.getVenueCalendarDay(venueId, date);
        return Result.success(result);
    }
}