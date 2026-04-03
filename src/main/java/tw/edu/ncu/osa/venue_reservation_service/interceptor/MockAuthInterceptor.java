package tw.edu.ncu.osa.venue_reservation_service.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.User;
import tw.edu.ncu.osa.venue_reservation_service.util.UserContext;

@Component
public class MockAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("Authorization");

        // MVP 階段：只要有這個 mock-token，就自動登入一個測試學生
        if ("mock-token-123".equals(token)) {
            User mockUser = new User("110502000", "中央大學測試生", "STUDENT", 1L);
            UserContext.setUser(mockUser);
            return true;
        }

        // 未來這裡會改成解析真正的 JWT 或 Portal Token
        response.setStatus(401);
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 請求處理完畢後，清空 ThreadLocal 確保安全
        UserContext.remove();
    }
}