package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 場地日曆月視圖 VO (Value Object)
 * 用於 API 回傳場地月份日曆資料，只顯示每日是否有預約，無時段詳情
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VenueCalendarMonthVO {
    
    // ==========================================
    // 基礎資訊
    // ==========================================
    
    /**
     * 年份
     */
    private Integer year;
    
    /**
     * 月份 (1-12)
     */
    private Integer month;
    
    // ==========================================
    // 月份日期集合
    // ==========================================
    
    /**
     * 該月所有日期的摘要資訊
     */
    private List<DaySimpleSummary> days;
    
    // ==========================================
    // 內部類：日期簡化摘要
    // ==========================================
    
    /**
     * 單一日期的簡化摘要（僅標記有無預約）
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DaySimpleSummary {
        
        /**
         * 日期，ISO 8601 格式 (例如: 2026-04-01)
         */
        private String date;
        
        /**
         * 是否有已通過審核的預約
         */
        private Boolean hasApprovedBooking;
        
        /**
         * 是否有用戶自己的預約（無論審核狀態）
         */
        private Boolean hasUserBooking;
    }
}

