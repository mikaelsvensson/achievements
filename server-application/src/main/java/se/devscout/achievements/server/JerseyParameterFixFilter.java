package se.devscout.achievements.server;

import javax.servlet.*;
import java.io.IOException;

/**
 * Servlet filter to deal with weird bug (?) explained here:
 * https://stackoverflow.com/questions/22376810/unable-to-retrieve-post-data-using-context-httpservletrequest-when-passed-to-o
 * <p>
 * The issue is that Jersey consumes request and/or form data. For some reason, invoking getParameterMap() prevents this behaviour.
 */
class JerseyParameterFixFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        request.getParameterMap(); // Workaround for bug.
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
}
