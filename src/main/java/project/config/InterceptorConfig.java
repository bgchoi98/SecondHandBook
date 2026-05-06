package project.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import project.config.interceptor.AdminAuthInterceptor;
import project.config.interceptor.MemberAuthInterceptor;
import project.config.interceptor.MemberActivityInterceptor;
import project.util.interceptor.UnreadInterceptor;

@Configuration
@RequiredArgsConstructor
public class InterceptorConfig implements WebMvcConfigurer {

    private final UnreadInterceptor unreadInterceptor;
    private final MemberAuthInterceptor memberAuthInterceptor;
    private final MemberActivityInterceptor memberActivityInterceptor;
    private final AdminAuthInterceptor adminAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/admin", "/admin/**")
                .excludePathPatterns(
                        "/admin/login",
                        "/admin/loginProcess",
                        "/admin/api/logout-pending",
                        "/admin/api/cancel-logout",
                        "/css/**", "/js/**", "/img/**"
                );

        registry.addInterceptor(unreadInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/js/**", "/images/**");

        registry.addInterceptor(memberAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/admin/**",
                        "/css/**",
                        "/js/**",
                        "/img/**",
                        "/images/**"
                );

        registry.addInterceptor(memberActivityInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/admin/**",
                        "/css/**",
                        "/js/**",
                        "/img/**",
                        "/api/member/logout-pending",
                        "/api/member/cancel-logout"
                );
    }
}
