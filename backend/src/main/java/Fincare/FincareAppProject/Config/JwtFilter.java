package Fincare.FincareAppProject.Config;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    // ✅ 블랙리스트(차단된 토큰 저장)
    private static final Set<String> BLACKLISTED_TOKENS = ConcurrentHashMap.newKeySet();

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7); // "Bearer " 이후의 토큰만 추출
            try {
                // ✅ 블랙리스트에 등록된 토큰인지 확인
                if (BLACKLISTED_TOKENS.contains(token)) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "로그아웃된 JWT입니다.");
                    return;
                }

                // ✅ JWT 토큰 검증 및 사용자명 추출
                String username = jwtUtil.validateTokenAndExtractUsername(token);

                // ✅ SecurityContext에 인증 정보 설정
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username, null, null // 현재 권한 기능 없음 (추후 필요 시 추가)
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }



    /**
     * ✅ 로그아웃 시 토큰을 블랙리스트에 추가하는 메서드
     */
    public static void blacklistToken(String token) {
        BLACKLISTED_TOKENS.add(token);
    }
}
