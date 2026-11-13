package com.resolveit.security;

import com.resolveit.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;

@Service
public class JwtService {

    private final AppProperties appProperties;
    private final SecretKey signingKey;

    public JwtService(AppProperties appProperties) {
        this.appProperties = appProperties;
        byte[] raw = appProperties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32) {
            raw = sha256Bytes(appProperties.jwt().secret());
        }
        this.signingKey = Keys.hmacShaKeyFor(raw);
    }

    public String generateAccessToken(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(appProperties.jwt().accessTtlSeconds());
        return Jwts.builder()
                .subject(String.valueOf(principal.getId()))
                .issuer(appProperties.jwt().issuer())
                .claim("role", principal.getRoleName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(appProperties.jwt().issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long extractUserId(String token) {
        try {
            Claims claims = parseAndValidate(token);
            return Long.parseLong(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public static String sha256Hex(String value) {
        return HexFormat.of().formatHex(sha256Bytes(value));
    }

    private static byte[] sha256Bytes(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

