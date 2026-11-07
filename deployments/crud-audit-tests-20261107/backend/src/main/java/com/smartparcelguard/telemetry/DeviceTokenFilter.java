package com.smartparcelguard.telemetry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparcelguard.common.ApiResponse;
import com.smartparcelguard.exception.ApiException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class DeviceTokenFilter extends OncePerRequestFilter {

    private final DeviceAuthenticationService authenticationService;
    private final ObjectMapper objectMapper;

    public DeviceTokenFilter(DeviceAuthenticationService authenticationService, ObjectMapper objectMapper) {
        this.authenticationService = authenticationService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/telemetry/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            DeviceSession.set(authenticationService.authenticate(request.getHeader(DeviceSession.TOKEN_HEADER)));
            filterChain.doFilter(request, response);
        } catch (ApiException exception) {
            response.setStatus(exception.getStatus().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), ApiResponse.fail(exception.getMessage(), null));
        } finally {
            DeviceSession.clear();
        }
    }
}
