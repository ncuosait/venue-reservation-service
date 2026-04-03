package tw.edu.ncu.osa.venue_reservation_service.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 全局統一回傳包裝類
 * @param <T> 回傳資料的類型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private boolean success; // 執行是否成功
    private String message;  // 提示訊息 (錯誤時顯示原因)
    private T data;          // 實際承載的數據

    /**
     * 成功回傳 - 帶數據
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(true, "操作成功", data);
    }

    /**
     * 成功回傳 - 無數據 (用於 Delete, Update 等操作)
     */
    public static <T> Result<T> success() {
        return new Result<>(true, "操作成功", null);
    }

    /**
     * 失敗回傳 - 自定義訊息
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(false, message, null);
    }
}