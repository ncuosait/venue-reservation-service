package tw.edu.ncu.osa.venue_reservation_service.controller;

import lombok.RequiredArgsConstructor;
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
        List<UnitVO> units = venueService.getAllUnits();
        return Result.success(units);
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
        List<VenueVO> venues = venueService.getVenuesByUnitId(unitId);
        return Result.success(venues);
    }

    /**
     * 取得單一場地的詳細資訊與可借用設備清單
     * @param id 場地 ID
     * @return 操作結果與場地詳細資訊
     */
    @GetMapping("/venues/{id}")
    public Result<VenueVO> getVenueById(@PathVariable Long id) {
        VenueVO venue = venueService.getVenueById(id);
        return Result.success(venue);
    }
}

