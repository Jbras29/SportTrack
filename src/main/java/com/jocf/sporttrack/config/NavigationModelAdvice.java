package com.jocf.sporttrack.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Expose le chemin de requête sans le contexte applicatif pour la navigation (sidebar).
 * Thymeleaf 3.1+ ne fournit plus {@code #request} par défaut dans les expressions.
 */
@ControllerAdvice
public class NavigationModelAdvice {

    @ModelAttribute("navRequestPath")
    public String navRequestPath(HttpServletRequest request) {
        String ctx = request.getContextPath();
        String uri = request.getRequestURI();
        if (ctx == null || ctx.isEmpty()) {
            return uri != null && !uri.isEmpty() ? uri : "/";
        }
        if (uri != null && uri.startsWith(ctx)) {
            String path = uri.substring(ctx.length());
            return path.isEmpty() ? "/" : path;
        }
        return uri != null ? uri : "/";
    }
}
