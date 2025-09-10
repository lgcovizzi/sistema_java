package com.example.sistemajava.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.UUID;

public class JwtCsrfTokenRepository implements CsrfTokenRepository {

    public static final String DEFAULT_HEADER_NAME = "X-CSRF-TOKEN";
    public static final String DEFAULT_PARAM_NAME = "_csrf";

    private final RsaKeyProvider rsaKeyProvider;
    private final long expirationMs;

    public JwtCsrfTokenRepository(RsaKeyProvider rsaKeyProvider, long expirationMs) {
        this.rsaKeyProvider = rsaKeyProvider;
        this.expirationMs = expirationMs;
    }

    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .setSubject("csrf")
                .setId(jti)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(rsaKeyProvider.getPrivateKey(), SignatureAlgorithm.RS256)
                .compact();
        return new org.springframework.security.web.csrf.DefaultCsrfToken(DEFAULT_HEADER_NAME, DEFAULT_PARAM_NAME, token);
    }

    @Override
    public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
        // Sem armazenamento no servidor; token auto-contido via JWT
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        String header = request.getHeader(DEFAULT_HEADER_NAME);
        if (!StringUtils.hasText(header)) return null;
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(rsaKeyProvider.getPublicKey())
                    .build()
                    .parseClaimsJws(header)
                    .getBody();
            if (claims.getExpiration().before(new Date())) return null;
            return new org.springframework.security.web.csrf.DefaultCsrfToken(DEFAULT_HEADER_NAME, DEFAULT_PARAM_NAME, header);
        } catch (Exception e) {
            return null;
        }
    }
}


