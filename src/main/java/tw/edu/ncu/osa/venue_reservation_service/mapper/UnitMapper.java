package tw.edu.ncu.osa.venue_reservation_service.mapper;

import org.apache.ibatis.annotations.Mapper;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.Unit;
import java.util.List;

/**
 * 管理單位資料持久層 Mapper
 * 負責與 units 資料表的交互
 */
@Mapper
public interface UnitMapper {
    
    // ==========================================
    // 查詢操作
    // ==========================================
    
    /**
     * 查詢所有管理單位紀錄
     * @return 所有單位實體清單
     */
    List<Unit> selectAllUnits();
}

