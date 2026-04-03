# Util 工具類設計文檔 (Utility Classes Design)

本文檔詳細說明 `util` 目錄下工具類的功能、設計原理與未來擴展方向。

---

## 1. UserContext

### 目前功能

`UserContext` 是線程本地存儲（ThreadLocal）工具類，負責在當前執行緒中存儲和管理用戶信息。主要功能包括：

| 功能項目 | 詳細說明 |
|---------|--------|
| **用戶信息存儲** | 利用 ThreadLocal 將當前用戶對象存儲在執行緒本地存儲中，確保每個執行緒擁有獨立的用戶上下文 |
| **用戶信息取得** | 提供 `getUser()` 方法隨時從當前執行緒中快速取得用戶對象，無需通過參數層層傳遞 |
| **上下文設置** | 提供 `setUser(User user)` 方法在執行緒開始時初始化用戶上下文，通常由 MockAuthInterceptor 或認證過濾器調用 |
| **上下文清理** | 提供 `remove()` 方法在請求結束後移除 ThreadLocal 數據，防止記憶體洩漏 |

**當前架構優勢：**
- ✅ 設計簡潔，易於使用
- ✅ 無需修改方法簽名即可跨層傳遞用戶信息
- ✅ 線程安全，每個請求有獨立的用戶上下文
- ✅ 已有清理機制提醒，降低記憶體洩漏風險

**應用場景：**
- Controller 中的業務邏輯可直接調用 `UserContext.getUser()` 取得當前用戶
- Service 層無需將用戶對象作為參數傳遞
- 審計日誌、權限檢查等可快速獲取當前操作人信息

**風險說明：**
⚠️ ThreadLocal 在 Web 應用中容易產生記憶體洩漏，特別是在線程池環境下。必須確保：
- 所有請求處理完成後調用 `remove()`
- 在 WebConfig 的攔截器 `afterCompletion()` 方法中進行清理

### 未來擴展方向

#### 1.1 增強的用戶上下文
- **目標**：存儲除了用戶對象外的更多上下文信息
- **實現方式**：
  - 創建 `UserContextVO` 或 `RequestContext` 類封裝用戶及請求相關的信息
  - 存儲內容包括：用戶 ID、用戶名、角色、部門、登入時間、請求 ID 等
  - 使用 `UserContext<RequestContext>` 泛型來支持不同類型的上下文
- **代碼示例**：
  ```java
  public class RequestContext {
      private User user;
      private String requestId;        // 用於追蹤
      private String clientIp;         // 客戶端 IP
      private Long requestStartTime;   // 用於性能監控
      private Map<String, Object> customData; // 擴展字段
  }
  
  public class UserContext {
      private static final ThreadLocal<RequestContext> CONTEXT_THREAD_LOCAL = new ThreadLocal<>();
      
      public static void setContext(RequestContext context) {
          CONTEXT_THREAD_LOCAL.set(context);
      }
      
      public static RequestContext getContext() {
          return CONTEXT_THREAD_LOCAL.get();
      }
  }
  ```

#### 1.2 自動化清理機制
- **目標**：消除手動調用 `remove()` 的必要性，防止開發者遺忘導致記憶體洩漏
- **實現方式**：
  - 實現 `WebRequestInterceptor` 接口的 `afterCompletion()` 方法自動清理
  - 創建 `@Aspect` AOP 切面，在方法執行完畢後自動調用清理邏輯
  - 使用 Servlet Filter 包裝請求，確保 finally 塊中執行清理
- **代碼示例**：
  ```java
  @Component
  public class UserContextCleanupInterceptor extends HandlerInterceptorAdapter {
      @Override
      public void afterCompletion(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   Object handler, 
                                   Exception ex) {
          UserContext.remove();  // 自動清理
      }
  }
  ```

#### 1.3 支持嵌套上下文
- **目標**：支持異步任務、線程池等場景下的上下文傳播
- **實現方式**：
  - 實現棧式的上下文管理，支持 push/pop 操作
  - 在非同步任務執行前，將當前上下文複製到新建線程
  - 使用 Spring 的 `@Async` 與 `TaskDecorator` 實現上下文傳播
  - 對於使用者定義的線程池，提供 `ContextAwareRunnable` 包裝器
- **代碼示例**：
  ```java
  // 線程池配置中
  @Bean
  public TaskExecutor taskExecutor() {
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setTaskDecorator(new ContextAwareTaskDecorator());
      return executor;
  }
  
  // 裝飾器實現
  public class ContextAwareTaskDecorator implements TaskDecorator {
      @Override
      public Runnable decorate(Runnable runnable) {
          RequestContext context = UserContext.getContext();
          return () -> {
              UserContext.setContext(context);
              try {
                  runnable.run();
              } finally {
                  UserContext.remove();
              }
          };
      }
  }
  ```

#### 1.4 監控與統計
- **目標**：追蹤 UserContext 的使用情況，發現潛在的記憶體洩漏
- **實現方式**：
  - 添加計數器：記錄 set/remove 的調用次數
  - 定期檢查未被清理的 ThreadLocal 數據
  - 集成 JVM 監控工具，檢測 ThreadLocal 引起的記憶體洩漏
  - 提供監控端點暴露當前激活的上下文數量
- **代碼示例**：
  ```java
  public class UserContext {
      private static final AtomicInteger ACTIVE_CONTEXTS = new AtomicInteger(0);
      
      public static void setUser(User user) {
          USER_THREAD_LOCAL.set(user);
          ACTIVE_CONTEXTS.incrementAndGet();
      }
      
      public static void remove() {
          USER_THREAD_LOCAL.remove();
          ACTIVE_CONTEXTS.decrementAndGet();
      }
      
      public static int getActiveContextCount() {
          return ACTIVE_CONTEXTS.get();
      }
  }
  ```

#### 1.5 Spring Security 集成
- **目標**：與 Spring Security 的 SecurityContext 統一管理用戶信息
- **實現方式**：
  - 同步 UserContext 與 Spring Security 的 SecurityContext
  - 在認證成功後同時設置兩個上下文
  - 統一的 API，簡化用戶信息取得邏輯
- **代碼示例**：
  ```java
  public class AuthenticationService {
      public void authenticate(User user) {
          // 設置 UserContext
          UserContext.setUser(user);
          
          // 同時設置 Spring Security
          Authentication auth = new UsernamePasswordAuthenticationToken(
              user, null, user.getAuthorities()
          );
          SecurityContextHolder.getContext().setAuthentication(auth);
      }
  }
  ```

#### 1.6 支持多租戶
- **目標**：在多租戶環境中隔離不同租戶的上下文
- **實現方式**：
  - 在 RequestContext 中添加 `tenantId` 字段
  - 在數據層查詢時自動過濾租戶數據
  - 提供租戶隔離的驗證邏輯
- **代碼示例**：
  ```java
  public class RequestContext {
      private User user;
      private String tenantId;
      
      public String getTenantId() {
          return tenantId;
      }
  }
  ```

---

## 2. BookingUtils

### 目前功能

`BookingUtils` 是預約系統專用的位元遮罩（Bit Mask）工具類，用於高效地處理時間段衝突檢測。主要功能包括：

| 功能項目 | 詳細說明 |
|---------|--------|
| **列表轉遮罩** | `convertToMask()` 將小時列表（如 `[8, 9, 10]`）轉換為 24-bit 整數遮罩，每一位代表一個小時 |
| **衝突檢測** | `isConflict()` 使用位與運算快速判斷兩個時段是否重疊，O(1) 時間複雜度 |
| **遮罩反轉** | `parseMaskToList()` 將整數遮罩轉換回小時列表，供前端顯示或進一步處理 |

**數據結構示例：**
```
時間段：[8, 9, 10] 小時
轉換過程：
  - 第 8 位設置：1 << 8 = 0x0100
  - 第 9 位設置：1 << 9 = 0x0200
  - 第 10 位設置：1 << 10 = 0x0400
  - 結合：0x0100 | 0x0200 | 0x0400 = 0x0700 = 1792

位掩碼示意圖（24 小時）：
位置：23  22  21  20  19  18  17  16  15  14  13  12  11  10  9  8  7  6  5  4  3  2  1  0
值：   0   0   0   0   0   0   0   0   0   0   0   0   0   1   1  1  0  0  0  0  0  0  0  0
時間：23  22  21  20  19  18  17  16  15  14  13  12  11  10  9  8  7  6  5  4  3  2  1  0
```

**當前架構優勢：**
- ✅ 時間複雜度低：轉換 O(n)、衝突檢測 O(1)
- ✅ 空間高效：使用單個整數代表 24 小時
- ✅ 易於擴展：位運算邏輯清晰
- ✅ 適合資料庫存儲：整數易於持久化

**應用場景：**
- 檢查預約時段是否與現有預約衝突
- 快速查詢場地某日期的可用時段
- 計算場地利用率（被預約時段數 / 24）

### 未來擴展方向

#### 2.1 支持更多粒度的時間單位
- **目標**：支持 30 分鐘、15 分鐘等更細的時間粒度
- **實現方式**：
  - 對於 30 分鐘粒度：使用 48-bit（24 × 2）
  - 對於 15 分鐘粒度：使用 96-bit（24 × 4）
  - 使用 `BitSet` 類取代 int，支持任意長度的位遮罩
  - 提供泛型方法支持不同粒度的轉換
- **代碼示例**：
  ```java
  public class BookingUtils {
      // 30 分鐘粒度
      public static long convertToMask30Min(List<Integer> halfHours) {
          long mask = 0;
          for (Integer halfHour : halfHours) {
              if (halfHour >= 0 && halfHour <= 47) {
                  mask |= (1L << halfHour);
              }
          }
          return mask;
      }
      
      // 任意粒度（使用 BitSet）
      public static BitSet convertToMaskBitSet(List<Integer> slots, int maxSlots) {
          BitSet mask = new BitSet(maxSlots);
          for (Integer slot : slots) {
              if (slot >= 0 && slot < maxSlots) {
                  mask.set(slot);
              }
          }
          return mask;
      }
  }
  ```

#### 2.2 添加區間計算方法
- **目標**：支持更多預約相關的計算，如空閒時段合併、時長計算等
- **實現方式**：
  - `getConflictSlots()` - 返回兩個時段的衝突時段
  - `mergeSlots()` - 合併多個時段，得到連續的可用時段
  - `getFreeSlotsCount()` - 計算空閒時段數量
  - `getUnavailableHours()` - 計算不可用時段（被預約 + 被禁用）
- **代碼示例**：
  ```java
  // 取得衝突時段
  public static List<Integer> getConflictSlots(int mask1, int mask2) {
      int conflictMask = mask1 & mask2;
      return parseMaskToList(conflictMask);
  }
  
  // 計算空閒時段數量
  public static int getFreeSlotsCount(int bookedMask) {
      return 24 - Integer.bitCount(bookedMask);
  }
  
  // 合併多個遮罩
  public static int mergeMasks(List<Integer> masks) {
      int merged = 0;
      for (Integer mask : masks) {
          merged |= mask;
      }
      return merged;
  }
  ```

#### 2.3 時段驗證與規範化
- **目標**：確保輸入時段的有效性和合理性
- **實現方式**：
  - `validateSlots()` - 驗證時段範圍（0-23）
  - `isConsecutiveSlots()` - 檢查時段是否連續
  - `normalizeSlots()` - 排序並去重時段列表
  - `isValidBookingDuration()` - 檢查預約時長是否符合規則（如最少 1 小時、最多 8 小時）
- **代碼示例**：
  ```java
  public static boolean validateSlots(List<Integer> slots) {
      if (slots == null || slots.isEmpty()) return false;
      return slots.stream().allMatch(slot -> slot >= 0 && slot <= 23);
  }
  
  public static boolean isConsecutiveSlots(List<Integer> slots) {
      if (slots.size() <= 1) return true;
      List<Integer> sorted = slots.stream().sorted().toList();
      for (int i = 0; i < sorted.size() - 1; i++) {
          if (sorted.get(i + 1) - sorted.get(i) != 1) {
              return false;
          }
      }
      return true;
  }
  ```

#### 2.4 複雜場景支持
- **目標**：支持更複雜的預約場景，如多場地、多日期、週期預約等
- **實現方式**：
  - 創建 `BookingSchedule` 或 `TimeSlotScheduler` 類支持多日期檢查
  - 支持「禁用時段」（如維護時間）與「已預約時段」分開管理
  - 添加「預訂政策」驗證，如提前預訂時間、最短預訂時長等
  - 支持「重複預訂」檢查，如週期性預約
- **代碼示例**：
  ```java
  public class TimeSlotScheduler {
      private Map<LocalDate, Integer> dailyBookedSlots;    // 已預約時段
      private Map<LocalDate, Integer> dailyUnavailableSlots; // 禁用時段
      
      public boolean isTimeSlotAvailable(LocalDate date, List<Integer> slots) {
          int bookedMask = dailyBookedSlots.getOrDefault(date, 0);
          int unavailableMask = dailyUnavailableSlots.getOrDefault(date, 0);
          int requestedMask = BookingUtils.convertToMask(slots);
          
          return !BookingUtils.isConflict(requestedMask, bookedMask | unavailableMask);
      }
      
      public void bookSlots(LocalDate date, List<Integer> slots) {
          int mask = BookingUtils.convertToMask(slots);
          int existing = dailyBookedSlots.getOrDefault(date, 0);
          dailyBookedSlots.put(date, existing | mask);
      }
  }
  ```

#### 2.5 效能優化與批量操作
- **目標**：支持大規模預約查詢和批量操作
- **實現方式**：
  - 實現批量衝突檢測，一次檢查多個預約
  - 緩存常用的遮罩計算結果
  - 使用 `long` 或 `BigInteger` 提高性能（如多日期場景）
  - 提供並行檢測接口
- **代碼示例**：
  ```java
  // 批量檢測衝突
  public static List<String> detectConflicts(List<Booking> bookings, 
                                             List<Integer> requestedSlots) {
      List<String> conflicts = new ArrayList<>();
      int requestedMask = BookingUtils.convertToMask(requestedSlots);
      
      for (Booking booking : bookings) {
          int bookingMask = BookingUtils.convertToMask(booking.getSlots());
          if (BookingUtils.isConflict(requestedMask, bookingMask)) {
              conflicts.add(booking.getId());
          }
      }
      return conflicts;
  }
  ```

#### 2.6 單位測試與文檔增強
- **目標**：提供完整的單元測試和使用文檔
- **實現方式**：
  - 為每個方法編寫單元測試，包括邊界情況
  - 添加詳細的 JavaDoc 註解，解釋位運算邏輯
  - 提供使用示例和最佳實踐指南
  - 添加性能基準測試（Benchmark），展示 vs. 其他方法的效能對比
- **代碼示例**：
  ```java
  /**
   * 檢查兩個時段是否衝突
   * 
   * 原理：使用位與運算 (Bitwise AND)，若結果非 0 表示至少有一位同時為 1
   * 時間複雜度：O(1)
   * 空間複雜度：O(1)
   * 
   * @param mask1 第一個時段的遮罩（24-bit 整數）
   * @param mask2 第二個時段的遮罩（24-bit 整數）
   * @return true 表示有衝突，false 表示無衝突
   * 
   * 範例：
   *   mask1 = 0b000001100 (08:00-10:00)
   *   mask2 = 0b001000110 (01:00-02:00 和 08:00-09:00)
   *   mask1 & mask2 = 0b000000100 (08:00-09:00 衝突)
   *   結果：true (衝突)
   */
  public static boolean isConflict(int mask1, int mask2) {
      return (mask1 & mask2) != 0;
  }
  ```

---

## Util 工具類間的協作關係

```
┌─────────────────────────────────────────────────────────────┐
│                     Util 工具類體系                         │
└─────────────────────────────────────────────────────────────┘

┌──────────────────────────┐
│    UserContext           │
│  (上下文管理)             │
└────────────┬─────────────┘
             │
             │ 提供用戶信息
             ▼
┌──────────────────────────────────────────────────┐
│  Service/Controller 業務邏輯                     │
│  - 直接取得當前用戶信息                          │
│  - 無需參數層層傳遞                              │
└────────────┬─────────────────────────────────────┘
             │
             │ 使用預約工具
             ▼
┌──────────────────────────────────────────────────┐
│  BookingUtils                                    │
│  (時間段衝突檢測)                                │
│  - 轉換時段列表為遮罩                            │
│  - 快速衝突檢測                                  │
│  - 遮罩反轉為列表                                │
└──────────────────────────────────────────────────┘
```

---

## Util 工具類的最佳實踐

### 設計原則

| 原則 | 說明 | 示例 |
|------|------|------|
| **單一職責** | 每個工具類只負責一類相關的操作 | UserContext 只處理用戶上下文，BookingUtils 只處理時段計算 |
| **無狀態設計** | 工具類應為無狀態的，所有方法應是 static | 避免在工具類中存儲實例變量 |
| **異常安全** | 工具類方法應提供邊界檢查和異常處理 | `convertToMask()` 驗證 slot 範圍 |
| **文檔完整** | 複雜邏輯應有詳細註解 | BookingUtils 的位運算應附帶說明 |
| **易於測試** | 工具方法應無副作用，便於單元測試 | 純函數式設計，無外部依賴 |

### 常見陷阱與解決方案

| 陷阱 | 風險 | 解決方案 |
|------|------|--------|
| ThreadLocal 未清理 | 記憶體洩漏 | 在攔截器 afterCompletion 中調用 remove() |
| 位遮罩溢出 | 邏輯錯誤 | 驗證輸入範圍（0-23 for hours） |
| 忽視邊界情況 | 意外的 Null | 處理空列表、null 輸入 |
| 混淆時段表示 | 計算錯誤 | 明確定義時段含義（8 表示 08:00-09:00） |

---

## 總結與建議

### 當前狀態
- ✅ 基礎工具完整，支持核心預約功能
- ✅ UserContext 設計簡潔，降低代碼耦合
- ✅ BookingUtils 高效，適合頻繁的衝突檢測

### 短期優先級 (Next Sprint)
1. **完善 UserContext 清理機制** - 在 WebConfig 攔截器中自動調用 remove()
2. **添加 BookingUtils 單元測試** - 覆蓋邊界情況
3. **增加方法文檔** - 詳細的 JavaDoc 和使用示例

### 中期優先級 (1-2 個月)
1. **擴展 UserContext** - 支持完整的 RequestContext
2. **增加 BookingUtils 驗證方法** - 確保輸入時段的有效性
3. **性能監控** - 在 UserContext 中添加活躍上下文計數

### 長期優先級 (3-6 個月)
1. **支持多粒度時間** - 30 分鐘、15 分鐘等
2. **複雜場景支持** - 多場地、多日期、禁用時段等
3. **異步上下文傳播** - 支持線程池和非同步任務


