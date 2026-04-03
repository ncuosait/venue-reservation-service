package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 預約申請 VO (Value Object)
 * 用於個人申請清單顯示時的回傳物件
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingVO {
    // ==========================================
    // 基礎資訊
    // ==========================================
    
    /**
     * 申請案編號
     */
    private Long id;
    
    /**
     * 場地名稱
     */
    private String venueName;
    
    /**
     * 預約日期
     */
    private LocalDate bookingDate;
    
    /**
     * 預約時段清單 (0-23)
     */
    private List<Integer> slots;
    
    /**
     * 申請狀態
     * 0: 撤回, 1: 審核中, 2: 通過, 3: 拒絕
     */
    private Integer status;
    
    /**
     * 申請時間
     */
    private LocalDateTime createdAt;
}

