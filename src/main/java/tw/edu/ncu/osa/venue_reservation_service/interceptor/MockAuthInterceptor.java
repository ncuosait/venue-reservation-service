package tw.edu.ncu.osa.venue_reservation_service.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.User;
import tw.edu.ncu.osa.venue_reservation_service.util.UserContext;

@Slf4j
@Component
public class MockAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 如果是跨域預檢請求 (OPTIONS)，直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        String requestPath = request.getRequestURI();
        String method = request.getMethod();
        
        log.info("【MockAuthInterceptor】[preHandle] 攔截請求 - method={}, path={}, token={}",
                method, requestPath, token);

        // MVP 階段：只要有這個 mock-token，就自動登入一個測試學生
        if ("mock-token-123".equals(token)) {
            User mockUser = new User("110502000", "中央大學測試生", "STUDENT", 1L);
            UserContext.setUser(mockUser);
            log.info("【MockAuthInterceptor】[preHandle] 用戶認證成功，userId={}，userName={}，role={}, unitId={}", 
                    mockUser.getUserId(), mockUser.getName(), mockUser.getRole(), mockUser.getUnitId());
            log.debug("【MockAuthInterceptor】[preHandle] 用戶物件已存入 ThreadLocal");
            return true;
        }

        // 未來這裡會改成解析真正的 JWT 或 Portal Token
        log.warn("【MockAuthInterceptor】[preHandle] 用戶認證失敗 - 無效或缺少授權令牌，token={}", token);
        response.setStatus(401);
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String requestPath = request.getRequestURI();
        int status = response.getStatus();
        
        if (ex != null) {
            log.error("【MockAuthInterceptor】[afterCompletion] 請求處理發生異常 - path={}, status={}, exception={}",
                    requestPath, status, ex.getMessage(), ex);
        } else {
            log.info("【MockAuthInterceptor】[afterCompletion] 請求處理完畢 - path={}, status={}", requestPath, status);
        }
        
        // 請求處理完畢後，清空 ThreadLocal 確保安全
        User user = UserContext.getUser();
        if (user != null) {
            log.debug("【MockAuthInterceptor】[afterCompletion] 清除 ThreadLocal 中的用戶數據，userId={}", user.getUserId());
        }
        UserContext.remove();
        log.debug("【MockAuthInterceptor】[afterCompletion] ThreadLocal 已清空");
    }
}