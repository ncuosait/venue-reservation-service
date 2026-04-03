# 統一響應類設計文件

**日期：** 2026-03-18  
**版本號：** V1.0 (MVP)
---

透過 `Result<T>` 類別，我們為前後端建立了一套**契約（Contract）**。無論業務邏輯多麼複雜，前端收到的 HTTP Response Body 永遠維持一致的結構，這能大幅降低前端在處理異步請求時的邏輯複雜度。

由於 `pom.xml` 已經引入了 **Lombok**，可以用簡潔的方式寫。

---

### 🛠️ 實作 `common.result.Result<T>`

請在 `src/main/java/tw/edu/ncu/osa/venue_reservation_service/common/result/Result.java` 建立以下內容：

```java
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
```

---

### 🧠 為什麼這樣設計？（Lead 的架構思維）

1.  **泛型設計 (`<T>`)：**
    * **工程思維：** **類型安全 (Type Safety)**。
    * 透過泛型，編譯器能檢查你塞進 `data` 的東西是否符合預期。這能避免在 Runtime 出現 `ClassCastException`。

2.  **靜態工廠方法 (`static success/error`)：**
    * **工程思維：** **語意化與封裝 (Semantic & Encapsulation)**。
    * 與其每次都 `new Result(...)`，直接呼叫 `Result.success(data)` 更具讀性。這也隱藏了物件創建的細節，未來若要增加回傳欄位（例如：Timestamp），你只需要改這幾個 static 方法，不需要改動全專案的 Controller。

3.  **配合 Lombok (`@Data`)：**
    * 利用 Lombok 自動生成 Getter/Setter，保持代碼乾淨，這在大型專案中能顯著減少「樣板代碼 (Boilerplate Code)」。

---

### 💡 如何在 Controller 中使用？

有了這個類別後，你的 Controller 會變得非常專業：

```java
@RestController
@RequestMapping("/api/venues")
public class VenueController {

    @GetMapping("/{id}")
    public Result<VenueVO> getVenue(@PathVariable Long id) {
        VenueVO venue = venueService.getById(id);
        if (venue != null) {
            return Result.success(venue); // 自動封裝成 { "success": true, "data": {...}, "message": "操作成功" }
        }
        return Result.error("找不到該場地資訊"); // { "success": false, "data": null, "message": "找不到該場地資訊" }
    }
}
```

---

### 🚀 下一步建議

現在通訊協定定好了，接下來有兩個分支可以跑：

1.  **全局異常處理 (Global Exception Handler)：**
    當你的 Service 層拋出 `RuntimeException` 時，我們需要一個捕獲器，自動將報錯轉化為 `Result.error("系統繁忙")` 回傳，而不是噴出黃色的錯誤網頁。
2.  **Mock 登入攔截器 (Auth Interceptor)：**
    在還沒串 Portal 之前，我們先寫一個攔截器，往 `UserContext` 塞入假用戶，讓你可以開始開發「場地預約」的業務邏輯。

**你想先處理哪一部分？我建議先從「全局異常處理」開始，這能保證你的 API 永遠不崩潰。**