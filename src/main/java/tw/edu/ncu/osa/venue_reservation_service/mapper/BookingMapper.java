package tw.edu.ncu.osa.venue_reservation_service.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.Booking;
import java.time.LocalDate;

@Mapper
public interface BookingMapper {
    // 檢查是否有衝突的已通過預約
    int countConflictingApprovedBookings(
            @Param("venueId") Long venueId,
            @Param("date") LocalDate date,
            @Param("mask") int mask
    );

    // 插入預約主表
    int insertBooking(Booking booking);

    // 插入預約設備關聯表
    int insertBookingEquipment(@Param("bookingId") Long bookingId, @Param("equipmentId") Long equipmentId);
}