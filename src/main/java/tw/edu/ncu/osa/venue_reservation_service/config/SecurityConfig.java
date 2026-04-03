package tw.edu.ncu.osa.venue_reservation_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 關閉 CSRF (開發 API 必做，否則 POST 會被擋)
                .csrf(csrf -> csrf.disable())
                // 2. 設定路徑權限
                .authorizeHttpRequests(auth -> auth
                        // 放行 Swagger 相關路徑
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        // 先放行所有 API，讓我們自己的 MockAuthInterceptor 來檢查 token
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}
