# 全局異常攔截器設計文件

**日期：** 2026-03-18  
**版本號：** V1.0 (MVP)
---

在軟體工程中，此為 **「優雅降級（Graceful Degradation）」** 與 **「防禦性編程（Defensive Programming）」**。

如果不處理異常，當後端發生錯誤時（例如資料庫斷線、空指標），Spring Boot 預設會回傳一個帶有堆疊追蹤（Stack Trace）的 HTML 頁面。這不僅對前端極不友善，還會曝露伺服器內部路徑，造成資安風險。

我們將利用 `@RestControllerAdvice` 來打造一個全局捕獲器，確保系統即便「崩潰」了，回傳給前端的依然是我們定義好的 `Result` 協定。

---

### 🛠️ 實作 `exception.GlobalExceptionHandler`

請在 `src/main/java/tw/edu/ncu/osa/venue_reservation_service/exception/GlobalExceptionHandler.java` 建立以下內容：

```java
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
        return Result.error("伺服器內部錯誤，請稍後再試");
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
```

---

### 🧠 Lead 的架構思維解析

#### 1. 資訊隱藏 (Information Hiding)
* **工程思維：** 我們在代碼中使用了 `log.error` 記錄了完整的錯誤堆疊（供開發者除錯），但回傳給前端的 `Result.error` 只有一句簡短的「伺服器內部錯誤」。
* **原因：** **不可對外洩漏系統細節**。你不需要讓使用者知道是哪一行 SQL 噴掉，你只需要告訴他「現在不行，晚點再試」。

#### 2. 自動化參數回饋
* **工程思維：** 透過攔截 `MethodArgumentNotValidException`，我們可以自動抓取 `pom.xml` 中引入的 `spring-boot-starter-validation` 所產生的錯誤訊息。
* **範例：** 如果學生預約人數填了 `-1`，你的 DTO 限制 `@Min(1)`，這個處理器會自動回傳 `{ "success": false, "message": "人數不可小於 1" }`。這讓 Controller 變得很乾淨，完全不需要寫 `if (count < 1) ...`。



---

### 💡 下一步：Mock 登入攔截器

現在「保險箱」已經裝好了（不會噴黃色頁面了），基礎建設的最後一塊拼圖就是 **身份識別**。

因為我們現在還沒有串接 NCU Portal，但開發業務邏輯時需要知道「是誰在預約」。我建議下一步實作一個簡單的 **`HandlerInterceptor`**：
1. 它會攔截所有請求。
2. 檢查是否有 Mock Token。
3. 將假用戶資訊存入 `ThreadLocal`（我們稱之為 `UserContext`）。

**你想先看如何實作這個 `UserContext` 工具類，還是直接寫攔截器（Interceptor）的邏輯？**