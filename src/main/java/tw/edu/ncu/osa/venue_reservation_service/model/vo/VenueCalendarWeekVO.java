package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 場地日曆周視圖 VO (Value Object)
 * 用於 API 回傳場地周份日曆資料，包含詳細的時段資訊
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VenueCalendarWeekVO {
    
    // ==========================================
    // 周份資訊
    // ==========================================
    
    /**
     * 周開始日期 (周一)，ISO 8601 格式
     */
    private String weekStart;
    
    /**
     * 周結束日期 (周日)，ISO 8601 格式
     */
    private String weekEnd;
    
    // ==========================================
    // 周內日期集合
    // ==========================================
    
    /**
     * 周內 7 日的詳細資訊
     */
    private List<DayDetailSummary> days;
    
    // ==========================================
    // 內部類：日期詳細摘要
    // ==========================================
    
    /**
     * 單一日期的詳細摘要（包含時段資訊）
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DayDetailSummary {
        
        /**
         * 日期，ISO 8601 格式 (例如: 2026-04-06)
         */
        private String date;
        
        /**
         * 星期幾，中文表示 (例如: 星期一)
         */
        private String dayOfWeek;
        
        /**
         * 已通過審核的預約時段列表 (0-23 表示 0:00-23:00 的 24 個小時)
         */
        private List<Integer> approvedSlots;
        
        /**
         * 用戶自己的預約時段列表 (0-23)
         */
        private List<Integer> userSlots;
    }
}

