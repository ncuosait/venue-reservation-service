package tw.edu.ncu.osa.venue_reservation_service.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.Booking;
import java.time.LocalDate;
import java.util.List;

/**
 * 預約資料持久層 Mapper
 * 負責預約案與相關設備的資料庫操作
 */
@Mapper
public interface BookingMapper {
    
    // ==========================================
    // 衝突檢查
    // ==========================================
    
    /**
     * 檢查是否有衝突的已通過預約
     * @param venueId 場地 ID
     * @param date 預約日期
     * @param mask 24-bit 時段遮罩
     * @return 衝突的已通過預約數量
     */
    int countConflictingApprovedBookings(
            @Param("venueId") Long venueId,
            @Param("date") LocalDate date,
            @Param("mask") int mask
    );

    // ==========================================
    // 基本 CRUD 操作
    // ==========================================
    
    /**
     * 插入預約主表
     * @param booking 預約實體
     * @return 受影響行數
     */
    int insertBooking(Booking booking);
    
    /**
     * 根據預約 ID 查詢預約資訊
     * @param id 預約案 ID
     * @return 預約實體物件
     */
    Booking selectById(@Param("id") Long id);
    
    /**
     * 根據用戶 ID 查詢該用戶所有預約申請
     * @param userId 用戶 ID
     * @return 預約實體列表，按建立時間倒序排列
     */
    List<Booking> selectByUserId(@Param("userId") String userId);

    // ==========================================
    // 更新操作
    // ==========================================
    
    /**
     * 帶樂觀鎖的狀態更新
     * 透過版本號驗證確保更新時資料未被他人修改
     * @param id 預約案 ID
     * @param newStatus 新狀態 (0:撤回, 1:審核中, 2:通過, 3:拒絕)
     * @param oldVersion 舊版本號
     * @return 受影響行數 (0 表示版本號不符，更新失敗)
     */
    int updateStatusWithVersion(
            @Param("id") Long id,
            @Param("newStatus") Integer newStatus,
            @Param("oldVersion") Integer oldVersion
    );
    
    /**
     * 更新預約的完整資訊
     * @param booking 包含更新資訊的預約實體
     * @return 受影響行數
     */
    int updateBooking(Booking booking);

    // ==========================================
    // 關聯操作
    // ==========================================
    
    /**
     * 插入預約設備關聯表
     * @param bookingId 預約案 ID
     * @param equipmentId 設備 ID
     * @return 受影響行數
     */
    int insertBookingEquipment(@Param("bookingId") Long bookingId, @Param("equipmentId") Long equipmentId);

    // ==========================================
    // 日曆視圖查詢
    // ==========================================

    /**
     * 按日期範圍查詢已通過審核的預約
     * 用於日曆視圖顯示場地的已占用時段
     * @param venueId 場地 ID
     * @param startDate 開始日期
     * @param endDate 結束日期
     * @return 預約實體列表，按日期升序排列
     */
    List<Booking> selectApprovedBookingsByDateRange(
            @Param("venueId") Long venueId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 按日期範圍查詢用戶在特定場地的所有預約
     * 用於日曆視圖顯示用戶的預約紀錄
     * @param userId 用戶 ID
     * @param venueId 場地 ID
     * @param startDate 開始日期
     * @param endDate 結束日期
     * @return 預約實體列表，按日期升序排列
     */
    List<Booking> selectUserBookingsByDateRange(
            @Param("userId") String userId,
            @Param("venueId") Long venueId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}