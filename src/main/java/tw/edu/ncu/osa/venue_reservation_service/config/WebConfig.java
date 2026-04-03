package tw.edu.ncu.osa.venue_reservation_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tw.edu.ncu.osa.venue_reservation_service.interceptor.MockAuthInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private MockAuthInterceptor mockAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(mockAuthInterceptor)
                .addPathPatterns("/api/**")         // 攔截所有 API 請求
                .excludePathPatterns("/api/public/**"); // 排除公開 API
    }

    /**
     * 配置 ObjectMapper Bean，用於 JSON 序列化/反序列化
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}