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
        log.info("【VenueService】[getAllUnits] 開始查詢所有管理單位");
        // 1. 查詢所有單位紀錄
        List<Unit> units = unitMapper.selectAllUnits();
        log.info("【VenueService】[getAllUnits] 從數據庫查詢到 {} 個單位", units.size());
        log.debug("【VenueService】[getAllUnits] 原始單位數據：{}", units);

        // 2. 將 Unit 實體轉換為 UnitVO
        List<UnitVO> unitVOs = new ArrayList<>();
        for (Unit unit : units) {
            UnitVO unitVO = new UnitVO();
            unitVO.setId(unit.getId());
            unitVO.setName(unit.getName());
            unitVO.setCode(unit.getCode());
            unitVOs.add(unitVO);
            log.debug("【VenueService】[getAllUnits] 轉換單位 - ID={}, 名稱={}, 代碼={}", 
                    unitVO.getId(), unitVO.getName(), unitVO.getCode());
        }
        
        log.info("【VenueService】[getAllUnits] 成功轉換 {} 個單位VO，準備返回", unitVOs.size());
        return unitVOs;
    }

    // ==========================================
    // 2. 場地查詢
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public List<VenueVO> getVenuesByUnitId(Long unitId) {
        log.info("【VenueService】[getVenuesByUnitId] 開始查詢單位 {} 的場地清單", unitId);
        // 1. 根據單位 ID 查詢場地清單
        // MyBatis 會自動透過 ResultMap 與 collection 填充設備清單
        List<Venue> venues = venueMapper.selectVenuesByUnitId(unitId);
        log.info("【VenueService】[getVenuesByUnitId] 從數據庫查詢到 {} 個場地，unitId={}", venues.size(), unitId);
        log.debug("【VenueService】[getVenuesByUnitId] 原始場地數據：{}", venues);
        
        // 2. 將 Venue 實體轉換為 VenueVO
        List<VenueVO> venueVOs = new ArrayList<>();
        for (Venue venue : venues) {
            VenueVO venueVO = convertVenueToVO(venue);
            venueVOs.add(venueVO);
            log.debug("【VenueService】[getVenuesByUnitId] 轉換場地 - ID={}, 名稱={}, 容納人數={}, 設備數={}", 
                    venueVO.getId(), venueVO.getName(), venueVO.getCapacity(), 
                    venueVO.getEquipments() != null ? venueVO.getEquipments().size() : 0);
        }
        
        log.info("【VenueService】[getVenuesByUnitId] 成功轉換 {} 個場地VO，unitId={}，準備返回", venueVOs.size(), unitId);
        return venueVOs;
    }

    @Override
    @Transactional(readOnly = true)
    public VenueVO getVenueById(Long id) {
        log.info("【VenueService】[getVenueById] 開始查詢場地詳細資訊，id={}", id);
        // 1. 根據場地 ID 查詢詳細資訊
        Venue venue = venueMapper.selectVenueById(id);
        log.debug("【VenueService】[getVenueById] 從數據庫查詢結果，id={}，場地對象={}", id, venue);
        
        // 2. 防禦性編程：檢查場地是否存在
        if (venue == null) {
            log.error("【VenueService】[getVenueById] 場地不存在，id={}", id);
            throw new RuntimeException("找不到該場地資訊");
        }
        
        // 3. 將 Venue 實體轉換為 VenueVO 並回傳
        VenueVO venueVO = convertVenueToVO(venue);
        log.info("【VenueService】[getVenueById] 成功查詢並轉換場地VO，id={}，名稱={}，設備數={}", 
                id, venueVO.getName(), venueVO.getEquipments() != null ? venueVO.getEquipments().size() : 0);
        log.debug("【VenueService】[getVenueById] 返回場地VO：{}", venueVO);
        return venueVO;
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
        log.debug("【VenueService】[convertVenueToVO] 開始轉換場地實體為VO，venueId={}", venue.getId());
        VenueVO venueVO = new VenueVO();
        venueVO.setId(venue.getId());
        venueVO.setUnitId(venue.getUnitId());
        venueVO.setName(venue.getName());
        venueVO.setCapacity(venue.getCapacity());
        venueVO.setDescription(venue.getDescription());
        log.debug("【VenueService】[convertVenueToVO] 設置基本場地信息 - ID={}, 名稱={}, 容納人數={}", 
                venue.getId(), venue.getName(), venue.getCapacity());
        
        // 轉換設備清單
        List<EquipmentVO> equipmentVOs = new ArrayList<>();
        if (venue.getEquipments() != null) {
            log.debug("【VenueService】[convertVenueToVO] 開始轉換設備清單，共 {} 個設備", venue.getEquipments().size());
            for (Equipment equipment : venue.getEquipments()) {
                EquipmentVO equipmentVO = new EquipmentVO();
                equipmentVO.setId(equipment.getId());
                equipmentVO.setName(equipment.getName());
                equipmentVOs.add(equipmentVO);
                log.debug("【VenueService】[convertVenueToVO] 轉換設備 - ID={}, 名稱={}", equipment.getId(), equipment.getName());
            }
        }
        venueVO.setEquipments(equipmentVOs);
        
        log.debug("【VenueService】[convertVenueToVO] 完成場地VO轉換，venueId={}，設備數={}", 
                venue.getId(), equipmentVOs.size());
        return venueVO;
    }
}

