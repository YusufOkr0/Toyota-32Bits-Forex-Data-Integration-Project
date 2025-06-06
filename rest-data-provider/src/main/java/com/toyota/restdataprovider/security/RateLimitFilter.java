package com.toyota.restdataprovider.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyota.restdataprovider.exception.ExceptionResponse;
import com.toyota.restdataprovider.service.abstracts.RateLimitService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String TOO_MANY_REQUEST_MESSAGE = "Too many requests!!! you have exceeded your rate limit. Please try again in a few seconds.";

    private final ObjectMapper objectMapper;
    private final RateLimitService rateLimitingService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String requestURI = request.getRequestURI();

        if(!requestURI.startsWith("/api/rates")){
            filterChain.doFilter(request,response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null) {

            String username = ((String) authentication.getPrincipal());
            Bucket usersBucket = rateLimitingService.getUsersBucket(username);

            ConsumptionProbe consumptionResult = usersBucket.tryConsumeAndReturnRemaining(1L);

            if (consumptionResult.isConsumed()) {
                response.addHeader("X-Rate-Limit-Remaining", String.valueOf(consumptionResult.getRemainingTokens()));
            } else {
                handleTooManyRequestError(consumptionResult, request, response);    // WRITE THE RESPONSE AND RETURN
                return;
            }
        }

        filterChain.doFilter(request, response);

    }


    private void handleTooManyRequestError(ConsumptionProbe consumptionResult,
                                           HttpServletRequest request,
                                           HttpServletResponse response) throws IOException {

        ExceptionResponse exceptionResponse = new ExceptionResponse(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                TOO_MANY_REQUEST_MESSAGE,
                request.getRequestURI(),
                LocalDateTime.now()
        );

        double waitTimeInSeconds = TimeUnit.NANOSECONDS.toSeconds(consumptionResult.getNanosToWaitForRefill());

        response.setHeader("X-Rate-Limit-Retry-After-Seconds",String.valueOf(waitTimeInSeconds));
        response.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());

        response.getWriter().write(objectMapper.writeValueAsString(exceptionResponse));


    }


}
