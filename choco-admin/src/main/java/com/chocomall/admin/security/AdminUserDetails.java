package com.chocomall.admin.security;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * A back-office user as seen by Spring Security. The authorities are the URL patterns of the resources granted
 * through the user's roles (for example {@code /product/**}). Serializable so it can live in a Redis cache.
 */
public class AdminUserDetails implements UserDetails, Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String username;
    private final String password;
    private final boolean enabled;
    private final List<String> resourcePatterns;

    public AdminUserDetails(Long id, String username, String password, boolean enabled, List<String> resourcePatterns) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.resourcePatterns = List.copyOf(resourcePatterns);
    }

    public Long getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return resourcePatterns.stream().map(SimpleGrantedAuthority::new).toList();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
