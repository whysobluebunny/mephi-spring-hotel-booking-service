package ru.mephi.abondarenko.booking.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.mephi.abondarenko.booking.domain.Role;
import ru.mephi.abondarenko.booking.domain.UserEntity;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final Duration tokenLifetime;

    public JwtService(
            @Value("${app.security.jwt-secret:12345678901234567890123456789012}") String secret,
            @Value("${app.security.token-lifetime-minutes:60}") long lifetimeMinutes
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        this.tokenLifetime = Duration.ofMinutes(lifetimeMinutes);
    }

    public String generateUserToken(UserEntity user) {
        return generateToken(String.valueOf(user.getId()), user.getUsername(), List.of(user.getRole().name()));
    }

    public String generateInternalToken() {
        return generateToken("internal-service", "booking-service", List.of(Role.INTERNAL.name()));
    }

    private String generateToken(String subject, String username, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claim("username", username)
                .claim("roles", roles)
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(now.plus(tokenLifetime)))
                .signWith(signingKey)
                .compact();
    }
}
