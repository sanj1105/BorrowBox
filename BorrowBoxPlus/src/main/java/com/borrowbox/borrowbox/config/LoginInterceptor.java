package com.borrowbox.borrowbox.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("currentUserId") != null) return true;

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.equals("/login") || path.equals("/register") || path.startsWith("/css/")
                || path.startsWith("/js/") || path.startsWith("/images/") || path.equals("/favicon.ico")
                || path.equals("/error")) {
            return true;
        }
        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }
}
