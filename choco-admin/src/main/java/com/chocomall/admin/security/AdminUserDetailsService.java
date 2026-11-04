package com.chocomall.admin.security;

import com.chocomall.model.entity.AdminUser;
import com.chocomall.model.entity.ApiResource;
import com.chocomall.model.entity.Role;
import com.chocomall.model.repository.AdminUserRepository;
import com.chocomall.model.repository.ApiResourceRepository;
import com.chocomall.model.repository.RoleRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads a staff member together with the resource patterns of their active roles. The result is cached because it
 * is read on every request; role, resource and account changes evict the cache (see {@code AdminCacheEvictor}).
 */
@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    public static final String CACHE = "adminDetails";

    private final AdminUserRepository admins;
    private final RoleRepository roles;
    private final ApiResourceRepository resources;

    @Override
    @Cacheable(value = CACHE, key = "#username")
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUser admin = admins.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user " + username));
        Set<Long> resourceIds = new LinkedHashSet<>();
        for (Role role : roles.findAllById(admin.getRoleIds())) {
            if (role.getStatus() == 1) {
                resourceIds.addAll(role.getResourceIds());
            }
        }
        List<String> patterns = resources.findAllById(resourceIds).stream().map(ApiResource::getUrl).distinct().toList();
        return new AdminUserDetails(admin.getId(), admin.getUsername(), admin.getPassword(), admin.getStatus() == 1, patterns);
    }
}
