package tw.edu.ncu.osa.venue_reservation_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tw.edu.ncu.osa.venue_reservation_service.common.result.Result;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.BookingRequestDTO;
import tw.edu.ncu.osa.venue_reservation_service.service.BookingService;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /**
     * 提交預約申請
     */
    @PostMapping
    public Result<Long> createBooking(@Valid @RequestBody BookingRequestDTO request) {
        Long bookingId = bookingService.createBooking(request);
        return Result.success(bookingId);
    }
}