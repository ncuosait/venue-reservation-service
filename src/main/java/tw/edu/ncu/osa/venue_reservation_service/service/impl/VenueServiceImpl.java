package tw.edu.ncu.osa.venue_reservation_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tw.edu.ncu.osa.venue_reservation_service.mapper.UnitMapper;
import tw.edu.ncu.osa.venue_reservation_service.mapper.VenueMapper;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.Unit;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.Venue;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.Equipment;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.UnitVO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueVO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentVO;
import tw.edu.ncu.osa.venue_reservation_service.service.VenueService;
import java.util.ArrayList;
import java.util.List;

/**
 * 場地與組織業務服務實現類
 * 負責管理場地、單位與設備相關的業務邏輯
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VenueServiceImpl implements VenueService {

    private final UnitMapper unitMapper;
    private final VenueMapper venueMapper;

    // ==========================================
    // 1. 單位查詢
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public List<UnitVO> getAllUnits() {
        // 1. 查詢所有單位紀錄
        List<Unit> units = unitMapper.selectAllUnits();
        
        // 2. 將 Unit 實體轉換為 UnitVO
        List<UnitVO> unitVOs = new ArrayList<>();
        for (Unit unit : units) {
            UnitVO unitVO = new UnitVO();
            unitVO.setId(unit.getId());
            unitVO.setName(unit.getName());
            unitVO.setCode(unit.getCode());
            unitVOs.add(unitVO);
        }
        
        return unitVOs;
    }

    // ==========================================
    // 2. 場地查詢
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public List<VenueVO> getVenuesByUnitId(Long unitId) {
        // 1. 根據單位 ID 查詢場地清單
        // MyBatis 會自動透過 ResultMap 與 collection 填充設備清單
        List<Venue> venues = venueMapper.selectVenuesByUnitId(unitId);
        
        // 2. 將 Venue 實體轉換為 VenueVO
        List<VenueVO> venueVOs = new ArrayList<>();
        for (Venue venue : venues) {
            VenueVO venueVO = convertVenueToVO(venue);
            venueVOs.add(venueVO);
        }
        
        return venueVOs;
    }

    @Override
    @Transactional(readOnly = true)
    public VenueVO getVenueById(Long id) {
        // 1. 根據場地 ID 查詢詳細資訊
        Venue venue = venueMapper.selectVenueById(id);
        
        // 2. 防禦性編程：檢查場地是否存在
        if (venue == null) {
            throw new RuntimeException("找不到該場地資訊");
        }
        
        // 3. 將 Venue 實體轉換為 VenueVO 並回傳
        return convertVenueToVO(venue);
    }

    // ==========================================
    // 3. 輔助方法 - 資料轉換
    // ==========================================

    /**
     * 將 Venue 實體轉換為 VenueVO 輸出物件
     * @param venue 場地實體
     * @return 場地輸出物件
     */
    private VenueVO convertVenueToVO(Venue venue) {
        VenueVO venueVO = new VenueVO();
        venueVO.setId(venue.getId());
        venueVO.setUnitId(venue.getUnitId());
        venueVO.setName(venue.getName());
        venueVO.setCapacity(venue.getCapacity());
        venueVO.setDescription(venue.getDescription());
        
        // 轉換設備清單
        List<EquipmentVO> equipmentVOs = new ArrayList<>();
        if (venue.getEquipments() != null) {
            for (Equipment equipment : venue.getEquipments()) {
                EquipmentVO equipmentVO = new EquipmentVO();
                equipmentVO.setId(equipment.getId());
                equipmentVO.setName(equipment.getName());
                equipmentVOs.add(equipmentVO);
            }
        }
        venueVO.setEquipments(equipmentVOs);
        
        return venueVO;
    }
}

