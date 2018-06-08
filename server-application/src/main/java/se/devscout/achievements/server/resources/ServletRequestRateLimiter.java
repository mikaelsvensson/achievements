package se.devscout.achievements.server.resources;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ServletRequestRateLimiter implements Filter {

    private final RateLimiter rateLimiter;

    public ServletRequestRateLimiter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (servletRequest instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            if (request.getMethod().equals("OPTIONS") || request.getMethod().equals("HEAD")) {
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }
        }

        final String ip = servletRequest.getRemoteAddr();

        boolean isAccepted = rateLimiter.accept(ip);

        if (isAccepted) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
            httpResponse.setStatus(429);
        }
    }

    @Override
    public void destroy() {
    }
}
