package com.lp.salesdashboard.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lp.salesdashboard.util.IpUtils;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!"POST".equals(request.getMethod())) return true;

        String ip = IpUtils.clientIp(request);
        Bucket bucket = buckets.get(ip, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(20)
                        .refillIntervally(20, Duration.ofMinutes(1))
                        .build())
                .build());

        if (bucket.tryConsume(1)) return true;

        log.warn("Rate limit exceeded for IP {} on {} {}", ip, request.getMethod(), request.getRequestURI());
        response.setStatus(429);
        response.setHeader("Retry-After", "60");
        response.getWriter().write("Too many requests");
        return false;
    }

}
