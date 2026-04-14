package com.vivance.hotel.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that logs every inbound request and outbound response.
 * Adds a traceId to the MDC for correlation across log lines.
 */
@Slf4j
@Component
@Order(1)
public class RequestLoggingConfig implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        // Set traceId in MDC so all log lines in this request carry it
        org.slf4j.MDC.put("traceId", traceId);
        response.setHeader("X-Trace-Id", traceId);

        long startTime = System.currentTimeMillis();
        log.info("--> {} {} [traceId={}]", request.getMethod(), request.getRequestURI(), traceId);

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("<-- {} {} {} {}ms [traceId={}]",
                    request.getMethod(), request.getRequestURI(),
                    response.getStatus(), duration, traceId);
            org.slf4j.MDC.clear();
        }
    }
}
