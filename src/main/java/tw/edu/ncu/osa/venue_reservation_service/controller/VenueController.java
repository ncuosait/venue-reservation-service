package tw.edu.ncu.osa.venue_reservation_service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tw.edu.ncu.osa.venue_reservation_service.common.result.Result;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.UnitVO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueVO;
import tw.edu.ncu.osa.venue_reservation_service.service.VenueService;
import java.util.List;

/**
 * 場地與組織 RESTful API 控制層
 * 提供場地、單位與設備資訊的公開查詢終端點
 */
@Slf4j
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    // ==========================================
    // 1. 單位相關 API
    // ==========================================

    /**
     * 取得所有管理單位清單
     * @return 操作結果與所有單位資訊
     */
    @GetMapping("/units")
    public Result<List<UnitVO>> getAllUnits() {
        log.info("【VenueController】收到請求：獲取所有管理單位清單");
        try {
            List<UnitVO> units = venueService.getAllUnits();
            log.info("【VenueController】成功獲取管理單位清單，共 {} 個單位", units.size());
            log.debug("【VenueController】返回單位數據：{}", units);
            return Result.success(units);
        } catch (Exception e) {
            log.error("【VenueController】獲取管理單位清單失敗", e);
            throw e;
        }
    }

    // ==========================================
    // 2. 場地相關 API
    // ==========================================

    /**
     * 根據單位 ID 查詢該單位下的場地清單
     * @param unitId 單位 ID (必填)
     * @return 操作結果與場地清單資訊
     */
    @GetMapping("/venues")
    public Result<List<VenueVO>> getVenuesByUnit(@RequestParam Long unitId) {
        log.info("【VenueController】收到請求：根據單位 ID 查詢場地清單，unitId={}", unitId);
        try {
            List<VenueVO> venues = venueService.getVenuesByUnitId(unitId);
            log.info("【VenueController】成功查詢到場地清單，unitId={}，共 {} 個場地", unitId, venues.size());
            log.debug("【VenueController】返回場地數據：{}", venues);
            return Result.success(venues);
        } catch (Exception e) {
            log.error("【VenueController】查詢場地清單失敗，unitId={}", unitId, e);
            throw e;
        }
    }

    /**
     * 取得單一場地的詳細資訊與可借用設備清單
     * @param id 場地 ID
     * @return 操作結果與場地詳細資訊
     */
    @GetMapping("/venues/{id}")
    public Result<VenueVO> getVenueById(@PathVariable Long id) {
        log.info("【VenueController】收到請求：獲取場地詳細資訊，id={}", id);
        try {
            VenueVO venue = venueService.getVenueById(id);
            log.info("【VenueController】成功獲取場地詳細資訊，id={}，場地名稱={}", id, venue.getName());
            log.debug("【VenueController】返回場地數據：{}", venue);
            return Result.success(venue);
        } catch (Exception e) {
            log.error("【VenueController】獲取場地詳細資訊失敗，id={}", id, e);
            throw e;
        }
    }
}

