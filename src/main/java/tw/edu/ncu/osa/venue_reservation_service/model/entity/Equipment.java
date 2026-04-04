package tw.edu.ncu.osa.venue_reservation_service.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 設備資訊實體類
 * 對應資料庫 equipments 表，管理系統內的設備主檔案
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Equipment {
    
    /**
     * 設備唯一識別碼
     */
    private Long id;
    
    /**
     * 設備名稱 (如：無線麥克風、投影機等)
     */
    private String name;
    
    /**
     * 建立時間
     */
    private LocalDateTime createdAt;
}

