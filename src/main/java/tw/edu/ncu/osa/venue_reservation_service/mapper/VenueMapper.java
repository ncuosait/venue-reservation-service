package tw.edu.ncu.osa.venue_reservation_service.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.Venue;
import java.util.List;

/**
 * 場地資訊資料持久層 Mapper
 * 負責與 venues 資料表的交互，包含場地與設備的關聯查詢
 */
@Mapper
public interface VenueMapper {
    
    // ==========================================
    // 查詢操作
    // ==========================================
    
    /**
     * 根據單位 ID 查詢該單位所屬的所有場地
     * 查詢結果包含每個場地關聯的設備清單
     * @param unitId 單位 ID
     * @return 該單位下的場地清單 (包含設備資訊)
     */
    List<Venue> selectVenuesByUnitId(@Param("unitId") Long unitId);
    
    /**
     * 根據場地 ID 查詢場地詳細資訊
     * 包含場地基本資料與可借用的設備清單
     * @param id 場地 ID
     * @return 場地詳細資訊
     */
    Venue selectVenueById(@Param("id") Long id);
}

