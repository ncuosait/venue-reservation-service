package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理單位輸出物件 (Value Object)
 * 用於 API 回傳給前端的單位資訊
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnitVO {
    
    /**
     * 單位唯一識別碼
     */
    private Long id;
    
    /**
     * 單位名稱
     */
    private String name;
    
    /**
     * 單位代碼
     */
    private String code;
}

