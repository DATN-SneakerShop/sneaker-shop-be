package com.sneakershop.backend.config;

import com.sneakershop.backend.entity.login.User;
import com.sneakershop.backend.entity.login.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {
    private final String JWT_SECRET = "sneaker_secret_key_2026";
    private final long JWT_EXPIRATION = 604800000L;

    // Sửa hàm này: Nhận vào Object User thay vì String username
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + JWT_EXPIRATION);

        // Lấy danh sách mã quyền (ví dụ: ["ADMIN", "SALES"])
        String roles = user.getRoles().stream()
                .map(Role::getCode)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("roles", roles) // Đưa quyền vào Token
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, JWT_SECRET)
                .compact();
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser().setSigningKey(JWT_SECRET).parseClaimsJws(token).getBody();
        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(JWT_SECRET).parseClaimsJws(authToken);
            return true;
        } catch (Exception ex) { return false; }
    }
}