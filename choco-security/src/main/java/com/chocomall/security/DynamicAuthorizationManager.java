package com.chocomall.security;

import java.util.List;
import java.util.function.Supplier;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.util.AntPathMatcher;

/**
 * Resource-based authorization for the back office. The authorities of an admin are the URL patterns of the
 * resources granted to their roles (for example {@code /product/**}); a request is allowed when one of them
 * matches its path. Unknown paths are denied, so a new endpoint is closed until a resource grants it.
 */
public class DynamicAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final List<String> authenticatedUrls;

    public DynamicAuthorizationManager(List<String> authenticatedUrls) {
        this.authenticatedUrls = authenticatedUrls;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return new AuthorizationDecision(false);
        }
        String path = context.getRequest().getRequestURI().substring(context.getRequest().getContextPath().length());
        if (authenticatedUrls.stream().anyMatch(p -> matcher.match(p, path))) {
            return new AuthorizationDecision(true);
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (matcher.match(authority.getAuthority(), path)) {
                return new AuthorizationDecision(true);
            }
        }
        return new AuthorizationDecision(false);
    }
}
