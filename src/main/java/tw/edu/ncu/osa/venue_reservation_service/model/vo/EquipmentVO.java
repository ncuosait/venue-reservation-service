package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 場地詳細資訊輸出物件 (Value Object)
 * 用於 API 回傳給前端的場地資訊，包含可借用的設備清單
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VenueVO {
    
    /**
     * 場地唯一識別碼
     */
    private Long id;
    
    /**
     * 所屬單位 ID
     */
    private Long unitId;
    
    /**
     * 場地名稱
     */
    private String name;
    
    /**
     * 容納人數上限
     */
    private Integer capacity;
    
    /**
     * 場地介紹或借用規則說明
     */
    private String description;
    
    /**
     * 該場地可供借用的設備清單
     */
    private List<EquipmentVO> equipments;
}

