package tw.edu.ncu.osa.venue_reservation_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tw.edu.ncu.osa.venue_reservation_service.common.result.Result;

/**
 * 全局異常處理器
 * 攔截所有 Controller 拋出的異常，並轉化為統一的 Result 格式
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 處理所有未知的 RuntimeException
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        log.error("系統發生運行時異常: ", e); // 在後端 Log 記錄詳細錯誤資訊
        log.error(e.getMessage()); // 顯示異常訊息
        return Result.error(e.getMessage());
    }

    /**
     * 處理參數校驗異常 (Validation)
     * 當前端傳來的 DTO 違反了 @NotNull, @Min 等註解時觸發
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldError().getDefaultMessage();
        log.warn("參數校驗失敗: {}", msg);
        return Result.error(msg);
    }

    /**
     * 處理最頂層的 Exception (最後一道防線)
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("發生未預期錯誤: ", e);
        return Result.error("系統繁忙，請聯絡管理員");
    }
}