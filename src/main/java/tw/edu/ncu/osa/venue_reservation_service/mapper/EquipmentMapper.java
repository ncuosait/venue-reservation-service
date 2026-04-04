package tw.edu.ncu.osa.venue_reservation_service.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.Equipment;
import java.util.List;

/**
 * 設備資訊資料持久層 Mapper
 * 負責與 equipments 資料表的交互
 */
@Mapper
public interface EquipmentMapper {
    
    // ==========================================
    // 查詢操作
    // ==========================================
    
    /**
     * 根據場地 ID 查詢該場地可供借用的所有設備
     * 透過 venue_equipment_map 表進行關聯查詢
     * @param venueId 場地 ID
     * @return 該場地可借用的設備清單
     */
    List<Equipment> selectEquipmentsByVenueId(@Param("venueId") Long venueId);
}

