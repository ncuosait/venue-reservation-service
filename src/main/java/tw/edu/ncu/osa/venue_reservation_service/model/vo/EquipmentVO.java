package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 設備資訊輸出物件 (Value Object)
 * 用於 API 回傳給前端的設備資訊
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EquipmentVO {
    
    /**
     * 設備唯一識別碼
     */
    private Long id;
    
    /**
     * 設備名稱
     */
    private String name;
}

