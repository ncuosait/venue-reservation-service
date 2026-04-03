package tw.edu.ncu.osa.venue_reservation_service.util;

import java.util.List;

/**
 * 預約系統位元遮罩工具類
 */
public class BookingUtils {

    /**
     * 將小時列表轉換為 24-bit Mask
     * 範例：slots [8, 9] 代表 08:00-10:00 -> 回傳 2^8 + 2^9 = 768
     */
    public static int convertToMask(List<Integer> slots) {
        if (slots == null || slots.isEmpty()) return 0;
        int mask = 0;
        for (Integer slot : slots) {
            if (slot >= 0 && slot <= 23) {
                mask |= (1 << slot); // 使用位位移運算設置對應位元
            }
        }
        return mask;
    }

    /**
     * 檢查兩個時段是否衝突
     * 邏輯：只要任一位元同時為 1，運算結果就不會是 0
     */
    public static boolean isConflict(int mask1, int mask2) {
        return (mask1 & mask2) != 0;
    }

    /**
     * (optional) 將 Mask 轉回 List<Integer>，供前端顯示
     */
    public static List<Integer> parseMaskToList(int mask) {
        List<Integer> slots = new java.util.ArrayList<>();
        for (int i = 0; i < 24; i++) {
            if (((mask >> i) & 1) == 1) {
                slots.add(i);
            }
        }
        return slots;
    }
}