package com.malitrans.transport.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class JwtTokenUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);
    private static final String ROLES_CLAIM = "roles";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * Generate JWT token with username and roles
     * @param username The username (subject)
     * @param roles List of role strings (e.g., ["SUPPLIER", "COMPANY_MANAGER"])
     * @return JWT token string
     */
    public String generateToken(String username, List<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .setSubject(username)
                .claim(ROLES_CLAIM, roles) // Add roles claim
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    /**
     * Legacy method for backward compatibility (generates token without roles)
     * @deprecated Use generateToken(String username, List<String> roles) instead
     */
    @Deprecated
    public String generateToken(String username) {
        return generateToken(username, List.of());
    }

    /**
     * Extract username from JWT token
     * @param token JWT token string
     * @return Username (subject)
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * Extract roles from JWT token
     * @param token JWT token string
     * @return List of role strings (e.g., ["SUPPLIER", "COMPANY_MANAGER"])
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Object rolesObj = claims.get(ROLES_CLAIM);
        
        if (rolesObj == null) {
            logger.debug("No roles claim found in token for user: {}", claims.getSubject());
            return List.of();
        }
        
        // Handle both List<String> and array formats
        if (rolesObj instanceof List) {
            return (List<String>) rolesObj;
        } else if (rolesObj instanceof String) {
            // Single role as string
            return List.of((String) rolesObj);
        }
        
        logger.warn("Unexpected roles format in token for user: {}", claims.getSubject());
        return List.of();
    }

    /**
     * Extract all claims from JWT token
     * @param token JWT token string
     * @return Claims object
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Validate JWT token (signature and expiration)
     * @param token JWT token string
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            
            // Check expiration
            Date expiration = claims.getExpiration();
            if (expiration.before(new Date())) {
                logger.debug("Token expired for user: {}", claims.getSubject());
                return false;
            }
            
            // If we reach here, token is valid (signature verified by parser)
            logger.debug("Token validated successfully for user: {}", claims.getSubject());
            return true;
        } catch (ExpiredJwtException e) {
            logger.debug("Token expired: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            logger.warn("Unsupported JWT token: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            logger.warn("Malformed JWT token: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            logger.warn("JWT token is empty or null: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error validating token: {}", e.getMessage());
            return false;
        }
    }
}
