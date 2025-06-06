package com.toyota.restdataprovider.security;

import com.toyota.restdataprovider.exception.security.CustomJwtAuthenticationEntryPoint;
import com.toyota.restdataprovider.exception.security.InvalidAuthenticationHeaderException;
import com.toyota.restdataprovider.exception.security.InvalidTokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Component
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_HEADER_MISSING = "Authentication header is missing or doesn't start with 'Bearer'.";
    private static final String JWT_TOKEN_BLANK = "JWT token is missing or blank.";
    private static final String USERNAME_INVALID = "Username extracted from the token is empty or invalid.";
    private static final String TOKEN_EXPIRED = "JWT token is expired. Please take a new token.";

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final CustomJwtAuthenticationEntryPoint authenticationEntryPoint;


    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        log.info("Incoming request to: {}", requestURI);

        if (requestURI.equals("/auth/register") ||
                requestURI.equals("/auth/login") ||
                requestURI.equals("/swagger-ui.html") ||
                requestURI.startsWith("/swagger-ui/") ||
                requestURI.startsWith("/v3/api-docs")
        ) {
            filterChain.doFilter(request, response);
            return;
        }


        try{
            final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

            log.debug("Authorization header: {}", authHeader);

            if(authHeader == null || !authHeader.startsWith(BEARER_PREFIX)){
                log.warn(AUTH_HEADER_MISSING);
                throw new InvalidAuthenticationHeaderException(AUTH_HEADER_MISSING);
            }

            final String jwtToken = authHeader.substring(7);

            if(jwtToken.isBlank()){
                log.warn("JWT token is blank");
                throw new InvalidTokenException(JWT_TOKEN_BLANK);
            }

            final String username = jwtUtil.extractUsername(jwtToken);
            log.debug("Extracted username from token: {}", username);

            if ((username == null) || username.isBlank()) {
                log.warn(USERNAME_INVALID);
                throw new InvalidTokenException(USERNAME_INVALID);
            }

            if(SecurityContextHolder.getContext().getAuthentication() == null){
                final UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if(jwtUtil.isTokenValid(jwtToken)){
                    log.info("JWT token is valid. Setting authentication for user: {}", username);
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails.getUsername(),
                            null,
                            userDetails.getAuthorities()    // No authorization for my case.[Empty list.]
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    log.warn("JWT token is invalid or expired for user: {}", username);
                    throw new InvalidTokenException(TOKEN_EXPIRED);
                }
            }


            filterChain.doFilter(request,response);

        } catch (AuthenticationException ex) {
            log.error("Authentication error during JWT filter: {}", ex.getMessage());
            authenticationEntryPoint.commence(request,response,ex);     // Catch the exception and redirect to the entry point.
        }
    }


}
