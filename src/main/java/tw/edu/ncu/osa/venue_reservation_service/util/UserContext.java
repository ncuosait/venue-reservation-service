package tw.edu.ncu.osa.venue_reservation_service.util;

import tw.edu.ncu.osa.venue_reservation_service.model.entity.User;

/**
 * 用戶上下文工具類：利用 ThreadLocal 存儲當前執行緒的用戶資訊
 */
public class UserContext {
    private static final ThreadLocal<User> USER_THREAD_LOCAL = new ThreadLocal<>();

    public static void setUser(User user) {
        USER_THREAD_LOCAL.set(user);
    }

    public static User getUser() {
        return USER_THREAD_LOCAL.get();
    }

    /**
     * 重要：請求結束後必須移除，否則在 Thread Pool 環境下會導致記憶洩漏和安全問題
     */
    public static void remove() {
        USER_THREAD_LOCAL.remove();
    }
}