package tw.edu.ncu.osa.venue_reservation_service.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 管理單位實體類
 * 對應資料庫 units 表，管理系統中的行政單位資訊
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Unit {
    
    /**
     * 單位唯一識別碼
     */
    private Long id;
    
    /**
     * 單位名稱 (如：學務處本部)
     */
    private String name;
    
    /**
     * 單位代碼 (如：STUA)，用於對接 Portal 身分驗證
     */
    private String code;
    
    /**
     * 建立時間
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新時間
     */
    private LocalDateTime updatedAt;
}

