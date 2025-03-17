package com.toyota.restdataprovider.security;

import com.toyota.restdataprovider.exception.security.InvalidTokenException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret.key}")
    private String SECRET_KEY;
    @Value("${jwt.token.expiration}")
    private long JWT_EXPIRATION;



    public String generateJwtToken(UserDetails userDetails){
        return Jwts.builder()
                .issuer("FOREX DATA PLATFORM")
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION))
                .signWith(getSecretKey())
                .compact();
    }

    public String extractUsername(String jwtToken){
        return extractClaim(jwtToken,Claims::getSubject);
    }

     public boolean isTokenValid(String jwtToken) {
        Date expirationAt = extractClaim(
                jwtToken,
                Claims::getExpiration
        );
        return expirationAt.after(new Date());
    }




    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractClaimsFromToken(String jwtToken) {
        try {
            return Jwts
                    .parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(jwtToken)
                    .getPayload();
        } catch (JwtException ex) {
            throw new InvalidTokenException(ex.getMessage());
        }
    }


    private SecretKey getSecretKey(){
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }


}
