package com.malitrans.transport.security;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenUtil tokenUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenUtil tokenUtil, UserDetailsService userDetailsService) {
        this.tokenUtil = tokenUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String token = header.substring(7);
                
                // Validate token signature and expiration
                if (!tokenUtil.validateToken(token)) {
                    logger.debug("Invalid JWT token (signature or expiration)");
                    filterChain.doFilter(request, response);
                    return;
                }
                
                // Extract username and roles from token (no DB call for roles)
                String username = tokenUtil.getUsernameFromToken(token);
                List<String> roles = tokenUtil.getRolesFromToken(token);
                
                logger.debug("JWT authentication - Username: {}, Roles: {}", username, roles);
                
                // Convert roles to Spring Security authorities (without ROLE_ prefix)
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority(role))
                        .collect(Collectors.toList());
                
                // Optional: Verify user exists and is enabled (lightweight check)
                // This ensures disabled users cannot authenticate even with valid token
                try {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (!userDetails.isEnabled()) {
                        logger.warn("User {} is disabled, rejecting authentication", username);
                        filterChain.doFilter(request, response);
                        return;
                    }
                } catch (Exception e) {
                    logger.warn("User {} not found or error loading user details: {}", username, e.getMessage());
                    // Continue without authentication - let SecurityConfig handle authorization
                    filterChain.doFilter(request, response);
                    return;
                }
                
                // Create authentication with authorities from token
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                logger.debug("Authentication set for user: {} with authorities: {}", username, authorities);
                
            } catch (ExpiredJwtException e) {
                // Token expired - handle gracefully, allow request to proceed
                // The filter chain will reject with 401 if it's a protected endpoint
                // For refresh-token endpoint, this allows it to proceed without authentication
                logger.debug("JWT token expired: {}", e.getMessage());
                request.setAttribute("expired", e.getMessage());
                // Do NOT set authentication - let the request proceed
            } catch (Exception e) {
                // Token invalid or other error - continue without authentication
                logger.warn("Cannot set user authentication: {}", e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}
