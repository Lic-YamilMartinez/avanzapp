package com.avanzapp.avanzapp.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // Clave secreta (en un entorno real, guardar en archivo seguro o variable de entorno)
    private final String SECRET_KEY = "MiClaveSuperSecretaParaJWTdeAvanzApp12345"; // mínimo 32 caracteres
    private final long EXPIRATION_TIME = 1000 * 60 * 60 * 10; // 10 horas

    private Key getKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    // Genera el token
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Extrae el username del token
    public String extractUsername(String token) {
        return parseClaims(token).getBody().getSubject();
    }

    // Verifica si el token es válido
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()));
    }

    private Jws<Claims> parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token);
    }
}
