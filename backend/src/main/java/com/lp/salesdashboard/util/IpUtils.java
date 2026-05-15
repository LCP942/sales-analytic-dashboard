package com.lp.salesdashboard.util;

import jakarta.servlet.http.HttpServletRequest;

public final class IpUtils {
    private IpUtils() {}

    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}
