package Fincare.FincareAppProject.Config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private static final String SECRET_KEY = "your-256-bit-secret-key-that-is-very-secure-and-long-enough"; // 최소 256비트 길이의 키
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 10; // 10시간

    private final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    // 토큰 생성
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 토큰 검증
    public String validateTokenAndExtractUsername(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            throw new RuntimeException("Invalid or expired JWT token");
        }
    }

    // ✅ 새롭게 추가된 `validateToken` 메서드 (토큰이 유효한지만 확인)
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true; // 토큰이 유효함
        } catch (JwtException | IllegalArgumentException e) {
            return false; // 토큰이 유효하지 않음
        }
    }
}
