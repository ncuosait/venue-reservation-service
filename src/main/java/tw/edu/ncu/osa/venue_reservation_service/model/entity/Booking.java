package tw.edu.ncu.osa.venue_reservation_service.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 預約申請實體類
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Booking {
    private Long id;              // 申請案編號
    private Long venueId;         // 關聯場地 ID
    private String userId;        // 申請人 NCU Portal ID
    private LocalDate bookingDate; // 預約日期
    private Integer timeSlots;    // 24-bit 位元遮罩時段
    private Integer status;       // 0:撤回, 1:審核中, 2:通過, 3:拒絕
    private String purpose;       // 使用用途
    private Integer pCount;       // 預估人數
    private String contactInfo;   // 聯絡人資訊 (JSON 格式字串)
    private Integer version;      // 樂觀鎖版本號
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}