package com.chocomall.admin.service;

import com.chocomall.admin.dto.AuthDtos.AdminInfo;
import com.chocomall.admin.dto.AuthDtos.AdminParam;
import com.chocomall.admin.dto.AuthDtos.AdminView;
import com.chocomall.admin.dto.AuthDtos.LoginParam;
import com.chocomall.admin.dto.AuthDtos.LoginResult;
import com.chocomall.admin.dto.AuthDtos.MenuNode;
import com.chocomall.admin.dto.AuthDtos.PasswordParam;
import com.chocomall.admin.security.AdminCacheEvictor;
import com.chocomall.admin.security.AdminUserDetails;
import com.chocomall.common.api.CommonPage;
import com.chocomall.common.api.ResultCode;
import com.chocomall.common.exception.ApiException;
import com.chocomall.common.exception.Asserts;
import com.chocomall.common.util.PageParams;
import com.chocomall.core.store.ExpiringStore;
import com.chocomall.model.entity.AdminLoginLog;
import com.chocomall.model.entity.AdminUser;
import com.chocomall.model.entity.Menu;
import com.chocomall.model.entity.Role;
import com.chocomall.model.repository.AdminLoginLogRepository;
import com.chocomall.model.repository.AdminUserRepository;
import com.chocomall.model.repository.MenuRepository;
import com.chocomall.model.repository.RoleRepository;
import com.chocomall.security.JwtTokenService;
import com.chocomall.security.SecurityProperties;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Staff accounts: sign-in with brute-force protection, profile, password and role assignment. */
@Service
@RequiredArgsConstructor
public class AdminAccountService {

    public static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final AdminUserRepository admins;
    private final RoleRepository roles;
    private final MenuRepository menus;
    private final AdminLoginLogRepository loginLogs;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokens;
    private final SecurityProperties security;
    private final ExpiringStore store;
    private final AdminCacheEvictor cacheEvictor;

    @Value("${chocomall.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${chocomall.login.lockout-minutes:15}")
    private int lockoutMinutes;

    @Transactional
    public LoginResult login(LoginParam param, String ip, String userAgent) {
        String key = "admin-login:" + param.username().toLowerCase();
        long failures = store.get(key).map(Long::parseLong).orElse(0L);
        if (failures >= maxAttempts) {
            throw new ApiException(ResultCode.TOO_MANY_REQUESTS,
                    "Too many failed sign-in attempts. Try again in " + lockoutMinutes + " minutes.");
        }
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(param.username(), param.password()));
        } catch (BadCredentialsException e) {
            store.increment(key, Duration.ofMinutes(lockoutMinutes));
            throw e;
        }
        store.delete(key);
        recordLogin(param.username(), ip, userAgent);
        return new LoginResult(tokens.generate(param.username()), security.jwt().tokenHead().trim(), tokens.expirationSeconds());
    }

    private void recordLogin(String username, String ip, String userAgent) {
        admins.findByUsername(username).ifPresent(admin -> {
            admin.setLoginTime(LocalDateTime.now());
            AdminLoginLog log = new AdminLoginLog();
            log.setAdminId(admin.getId());
            log.setIp(ip);
            log.setUserAgent(userAgent == null ? null : userAgent.substring(0, Math.min(userAgent.length(), 500)));
            loginLogs.save(log);
            admins.save(admin);
        });
    }

    @Transactional(readOnly = true)
    public AdminInfo info(String username) {
        AdminUser admin = Asserts.found(admins.findByUsername(username), "Account");
        List<Role> active = roles.findAllById(admin.getRoleIds()).stream().filter(r -> r.getStatus() == 1).toList();
        Set<Long> menuIds = new LinkedHashSet<>();
        active.forEach(r -> menuIds.addAll(r.getMenuIds()));
        return new AdminInfo(admin.getId(), admin.getUsername(), admin.getNickName(), admin.getIcon(),
                active.stream().map(Role::getName).toList(), menuTree(menus.findAllById(menuIds)));
    }

    @Transactional
    public AdminView create(AdminParam param) {
        Asserts.isTrue(param.password() != null && !param.password().isBlank(), "A password is required");
        Asserts.state(!admins.existsByUsername(param.username()), "That username is already taken");
        AdminUser a = new AdminUser();
        a.setUsername(param.username());
        a.setPassword(passwordEncoder.encode(param.password()));
        copy(param, a);
        return AdminView.from(admins.save(a));
    }

    @Transactional
    public AdminView update(Long id, AdminParam param) {
        AdminUser a = load(id);
        if (param.status() != null && param.status() == 0) {
            guardLastSuperAdmin(a);
        }
        copy(param, a);
        cacheEvictor.evict(a.getUsername());
        return AdminView.from(a);
    }

    @Transactional
    public void updateStatus(Long id, int status, String actor) {
        AdminUser a = load(id);
        if (status == 0) {
            Asserts.isTrue(!a.getUsername().equals(actor), "You cannot disable your own account");
            guardLastSuperAdmin(a);
        }
        a.setStatus(status);
        cacheEvictor.evict(a.getUsername());
    }

    @Transactional
    public void delete(Long id, String actor) {
        AdminUser a = load(id);
        Asserts.isTrue(!a.getUsername().equals(actor), "You cannot delete your own account");
        guardLastSuperAdmin(a);
        admins.delete(a);
        refreshRoleCounts();
        cacheEvictor.evict(a.getUsername());
    }

    @Transactional
    public void changePassword(PasswordParam param, String actor, boolean canResetOthers) {
        AdminUser a = Asserts.found(admins.findByUsername(param.username()), "Account");
        if (a.getUsername().equals(actor)) {
            Asserts.isTrue(param.oldPassword() != null && passwordEncoder.matches(param.oldPassword(), a.getPassword()),
                    "The current password is incorrect");
        } else {
            Asserts.isTrue(canResetOthers, "You can only change your own password");
        }
        a.setPassword(passwordEncoder.encode(param.newPassword()));
        cacheEvictor.evict(a.getUsername());
    }

    @Transactional
    public void assignRoles(Long adminId, Set<Long> roleIds) {
        AdminUser a = load(adminId);
        Role superAdmin = roles.findByName(SUPER_ADMIN).orElse(null);
        if (superAdmin != null && a.getRoleIds().contains(superAdmin.getId()) && !roleIds.contains(superAdmin.getId())) {
            guardLastSuperAdmin(a);
        }
        Asserts.isTrue(roles.findAllById(roleIds).size() == roleIds.size(), "One of the roles does not exist");
        a.getRoleIds().clear();
        a.getRoleIds().addAll(roleIds);
        admins.saveAndFlush(a);
        refreshRoleCounts();
        cacheEvictor.evict(a.getUsername());
    }

    @Transactional(readOnly = true)
    public CommonPage<AdminView> list(String keyword, Integer pageNum, Integer pageSize) {
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return CommonPage.of(admins.search(kw, PageParams.of(pageNum, pageSize)), AdminView::from);
    }

    @Transactional(readOnly = true)
    public AdminView get(Long id) {
        return AdminView.from(load(id));
    }

    @Transactional(readOnly = true)
    public Set<Long> roleIdsOf(Long adminId) {
        return load(adminId).getRoleIds();
    }

    public boolean isSuperAdmin(AdminUserDetails d) {
        return admins.findById(d.getId()).map(a -> roles.findByName(SUPER_ADMIN)
                .map(r -> a.getRoleIds().contains(r.getId())).orElse(false)).orElse(false);
    }

    // -------------------------------------------------------------------------------- helpers

    private AdminUser load(Long id) {
        return Asserts.found(admins.findById(id), "Account");
    }

    private void copy(AdminParam p, AdminUser a) {
        a.setNickName(p.nickName());
        a.setEmail(p.email());
        a.setIcon(p.icon());
        a.setNote(p.note());
        if (p.status() != null) {
            a.setStatus(p.status());
        }
    }

    /** The system must always keep at least one active super admin. */
    private void guardLastSuperAdmin(AdminUser candidate) {
        roles.findByName(SUPER_ADMIN).ifPresent(superRole -> {
            if (candidate.getRoleIds().contains(superRole.getId()) && admins.countByRole(superRole.getId()) <= 1) {
                throw new ApiException(ResultCode.CONFLICT, "At least one super admin must remain");
            }
        });
    }

    private void refreshRoleCounts() {
        for (Role r : roles.findAll()) {
            r.setAdminCount((int) admins.countByRole(r.getId()));
        }
    }

    static List<MenuNode> menuTree(List<Menu> all) {
        Map<Long, MenuNode> nodes = new LinkedHashMap<>();
        all.stream().sorted((a, b) -> Integer.compare(b.getSort(), a.getSort())).forEach(m ->
                nodes.put(m.getId(), new MenuNode(m.getId(), m.getParentId(), m.getTitle(), m.getLevel(), m.getSort(),
                        m.getName(), m.getIcon(), m.isHidden(), new ArrayList<>())));
        List<MenuNode> roots = new ArrayList<>();
        for (MenuNode n : nodes.values()) {
            MenuNode parent = nodes.get(n.parentId());
            if (parent != null) {
                parent.children().add(n);
            } else {
                roots.add(n);
            }
        }
        return roots;
    }
}
