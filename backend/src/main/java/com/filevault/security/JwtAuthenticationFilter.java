package com.filevault.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtProvider jwtProvider;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) 
            throws ServletException, IOException {
        
        try {
            String jwt = getJwtFromRequest(request);

            if (!StringUtils.hasText(request.getHeader("Authorization"))) {
                logger.debug("No Authorization header present for request " + request.getMethod() + " " + request.getRequestURI());
            } else {
                String bearer = request.getHeader("Authorization");
                String tokenOnly = bearer != null && bearer.startsWith("Bearer ") ? bearer.substring(7) : bearer;
                String preview = tokenOnly != null ? (tokenOnly.length() > 16 ? tokenOnly.substring(0, 8) + "..." + tokenOnly.substring(tokenOnly.length() - 8) : tokenOnly) : "<null>";
                logger.debug("Authorization header present (preview=" + preview + ") for request " + request.getMethod() + " " + request.getRequestURI());
            }

            if (StringUtils.hasText(jwt)) {
                String tokenPreview = jwt.length() > 16 ? jwt.substring(0, 8) + "..." + jwt.substring(jwt.length() - 8) : jwt;
                try {
                    if (jwtProvider.validateToken(jwt)) {
                        String username = jwtProvider.getUsernameFromToken(jwt);
                        logger.debug("Validated JWT (preview=" + tokenPreview + ") - username=" + username + " for request " + request.getMethod() + " " + request.getRequestURI());

                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        logger.debug("Authentication set for user=" + username + " authorities=" + userDetails.getAuthorities() + " remote=" + request.getRemoteAddr());
                    } else {
                        logger.debug("JWT validation returned false (preview=" + tokenPreview + ") for request " + request.getMethod() + " " + request.getRequestURI());
                    }
                } catch (Exception ex) {
                    logger.warn("JWT processing failed (preview=" + tokenPreview + ") for request " + request.getMethod() + " " + request.getRequestURI() + " remote=" + request.getRemoteAddr() + ": " + ex.getMessage(), ex);
                    logger.debug("JWT processing stacktrace", ex);
                }
            }
        } catch (Exception e) {
            String bearer = request.getHeader("Authorization");
            String tokenOnly = bearer != null && bearer.startsWith("Bearer ") ? bearer.substring(7) : bearer;
            String preview = tokenOnly != null ? (tokenOnly.length() > 16 ? tokenOnly.substring(0, 8) + "..." + tokenOnly.substring(tokenOnly.length() - 8) : tokenOnly) : "<null>";
            logger.error("Could not set user authentication (tokenPreview=" + preview + ", path=" + request.getRequestURI() + ", remote=" + request.getRemoteAddr() + ")", e);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
