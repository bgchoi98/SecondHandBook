package project.config.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;
import project.member.MemberVO;
import project.util.Const;
import project.util.LoginUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class MemberAuthInterceptor implements HandlerInterceptor {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final List<AuthRule> authRules = Arrays.asList(
            AuthRule.any("/mypage/**"),
            AuthRule.any("/profile/address/**"),
            AuthRule.any("/profile/bookclub/list"),
            AuthRule.any("/profile/wishlist/**"),
            AuthRule.of("POST", "/member/update"),
            AuthRule.of("GET", "/member/delete"),

            AuthRule.of("GET", "/trade"),
            AuthRule.of("POST", "/trade"),
            AuthRule.any("/trade/modify/*"),
            AuthRule.of("POST", "/trade/delete/*"),
            AuthRule.of("POST", "/trade/like"),
            AuthRule.of("POST", "/trade/sold"),
            AuthRule.of("POST", "/trade/confirm/*"),

            AuthRule.any("/payments/**"),

            AuthRule.any("/chatrooms"),
            AuthRule.any("/chat/rooms/list"),
            AuthRule.any("/chat/messages"),
            AuthRule.any("/chat/image/upload"),
            AuthRule.any("/chat/memberInfo"),

            AuthRule.of("POST", "/bookclubs"),
            AuthRule.any("/bookclubs/*/manage/**"),
            AuthRule.of("GET", "/bookclubs/*/edit"),
            AuthRule.of("POST", "/bookclubs/*/join-requests"),
            AuthRule.of("POST", "/bookclubs/*/join"),
            AuthRule.of("POST", "/bookclubs/*/wish"),
            AuthRule.of("POST", "/bookclubs/*/leave"),
            AuthRule.of("POST", "/bookclubs/*/boards/*/like"),
            AuthRule.of("POST", "/bookclubs/*/posts"),
            AuthRule.of("GET", "/bookclubs/*/posts/*/edit"),
            AuthRule.of("POST", "/bookclubs/*/posts/*/edit"),
            AuthRule.of("POST", "/bookclubs/*/posts/*/delete"),
            AuthRule.of("POST", "/bookclubs/*/posts/*/comments"),
            AuthRule.of("POST", "/bookclubs/*/posts/*/comments/*/edit"),
            AuthRule.of("POST", "/bookclubs/*/posts/*/comments/*/delete")
    );

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String uri = normalizeUri(request);
        String method = request.getMethod();

        if (!requiresMemberAuth(method, uri)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        MemberVO member = session == null ? null : (MemberVO) session.getAttribute(Const.SESSION);
        if (member != null) {
            return true;
        }

        log.info("Member authentication required: method={}, uri={}", method, uri);
        return handleUnauthorized(request, response);
    }

    private boolean requiresMemberAuth(String method, String uri) {
        return authRules.stream().anyMatch(rule -> rule.matches(method, uri, pathMatcher));
    }

    private String normalizeUri(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }

    private boolean handleUnauthorized(HttpServletRequest request,
                                       HttpServletResponse response) throws Exception {
        if (isAjaxOrJsonRequest(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"로그인이 필요합니다.\",\"redirectUrl\":\"/login\"}");
            return false;
        }

        String currentUrl = LoginUtil.getCurrentUrl(request);
        String encodedUrl = URLEncoder.encode(currentUrl, StandardCharsets.UTF_8);
        response.sendRedirect(request.getContextPath() + "/login?redirect=" + encodedUrl);
        return false;
    }

    private boolean isAjaxOrJsonRequest(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        String contentType = request.getContentType();
        String accept = request.getHeader("Accept");

        return "XMLHttpRequest".equals(requestedWith)
                || (contentType != null && contentType.contains("application/json"))
                || (accept != null && accept.contains("application/json"));
    }

    private static class AuthRule {
        private final String method;
        private final String pattern;

        private AuthRule(String method, String pattern) {
            this.method = method;
            this.pattern = pattern;
        }

        static AuthRule any(String pattern) {
            return new AuthRule(null, pattern);
        }

        static AuthRule of(String method, String pattern) {
            return new AuthRule(method, pattern);
        }

        boolean matches(String requestMethod, String uri, AntPathMatcher pathMatcher) {
            return (method == null || method.equalsIgnoreCase(requestMethod))
                    && pathMatcher.match(pattern, uri);
        }
    }
}
