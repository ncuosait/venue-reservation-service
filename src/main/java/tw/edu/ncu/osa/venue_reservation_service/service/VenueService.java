package tw.edu.ncu.osa.venue_reservation_service.service;

import tw.edu.ncu.osa.venue_reservation_service.model.vo.UnitVO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueVO;
import java.util.List;

/**
 * 場地與組織業務服務介面
 * 定義場地、單位與設備查詢相關的業務操作契約
 */
public interface VenueService {
    
    // ==========================================
    // 單位查詢
    // ==========================================
    
    /**
     * 獲取所有管理單位清單
     * @return 所有單位的輸出物件清單
     */
    List<UnitVO> getAllUnits();
    
    // ==========================================
    // 場地查詢
    // ==========================================
    
    /**
     * 根據單位 ID 查詢該單位下屬的所有場地
     * @param unitId 單位 ID
     * @return 該單位的場地清單 (包含設備資訊)
     */
    List<VenueVO> getVenuesByUnitId(Long unitId);
    
    /**
     * 根據場地 ID 查詢場地詳細資訊
     * @param id 場地 ID
     * @return 場地詳細資訊與可借用設備清單
     * @throws RuntimeException 當場地不存在時拋出
     */
    VenueVO getVenueById(Long id);
}

