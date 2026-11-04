package com.chocomall.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Populates the security context from the bearer token. Intentionally not a Spring bean: a bean would also be
 * registered as a plain servlet filter and run before the security chain, whose context handling would discard it.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService tokens;
    private final UserDetailsService users;
    private final String header;
    private final String tokenHead;

    public JwtAuthenticationFilter(JwtTokenService tokens, UserDetailsService users, SecurityProperties props) {
        this.tokens = tokens;
        this.users = users;
        this.header = props.jwt().header();
        this.tokenHead = props.jwt().tokenHead();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String value = request.getHeader(header);
        if (value != null && value.startsWith(tokenHead)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            tokens.validate(value.substring(tokenHead.length())).ifPresent(username -> {
                try {
                    UserDetails user = users.loadUserByUsername(username);
                    if (user.isEnabled()) {
                        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                } catch (RuntimeException ignored) {
                    // unknown or disabled user: stay unauthenticated, the entry point answers 401
                }
            });
        }
        chain.doFilter(request, response);
    }
}
