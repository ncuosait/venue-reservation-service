package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 場地日曆日視圖 VO (Value Object)
 * 用於 API 回傳場地單日日曆資料，包含最詳細的時段與預約資訊
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VenueCalendarDayVO {
    
    // ==========================================
    // 場地與日期資訊
    // ==========================================
    
    /**
     * 場地 ID
     */
    private Long venueId;
    
    /**
     * 場地名稱
     */
    private String venueName;
    
    /**
     * 日期，ISO 8601 格式 (例如: 2026-04-06)
     */
    private String date;
    
    /**
     * 星期幾，中文表示 (例如: 星期一)
     */
    private String dayOfWeek;
    
    // ==========================================
    // 時段占用情況
    // ==========================================
    
    /**
     * 已通過審核的預約時段列表 (0-23 表示 0:00-23:00 的 24 個小時)
     */
    private List<Integer> approvedSlots;
    
    /**
     * 用戶自己的預約時段列表 (0-23)
     */
    private List<Integer> userSlots;
    
    // ==========================================
    // 用戶預約詳情
    // ==========================================
    
    /**
     * 該日期用戶所有預約的詳細資訊
     */
    private List<UserBookingDetail> userBookingDetails;
    
    // ==========================================
    // 內部類：用戶預約詳情
    // ==========================================
    
    /**
     * 用戶單筆預約的詳細資訊
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserBookingDetail {
        
        /**
         * 預約案編號
         */
        private Long bookingId;
        
        /**
         * 該筆預約的時段列表 (0-23)
         */
        private List<Integer> slots;
        
        /**
         * 預約狀態：0=撤回, 1=審核中, 2=通過, 3=拒絕
         */
        private Integer status;
        
        /**
         * 使用用途
         */
        private String purpose;
        
        /**
         * 預約建立時間
         */
        private LocalDateTime createdAt;
    }
}

