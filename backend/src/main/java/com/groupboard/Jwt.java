package com.groupboard;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component public class Jwt {
    private final SecretKey key;
    Jwt(@Value("${app.jwt.secret}") String s) { key = Keys.hmacShaKeyFor(s.getBytes()); }
    String issue(AppUser u) {
        return Jwts.builder().subject(u.username).claim("role", u.role)
            .expiration(new Date(System.currentTimeMillis() + 8 * 3600_000L)).signWith(key).compact();
    }
    Claims read(String t) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(t).getPayload(); }
}
