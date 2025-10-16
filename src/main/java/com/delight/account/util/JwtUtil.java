package com.delight.account.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class JwtUtil {

    public static String generateToken(Long userId, Long accountId, String jwtSecret, long jwtExpiration) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("accountId", accountId)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(jwtExpiration, ChronoUnit.MILLIS)))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}