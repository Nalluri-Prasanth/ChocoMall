package com.chocomall.admin.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Access to the signed-in staff member, used to stamp audit trails. */
public final class CurrentAdmin {

    private CurrentAdmin() {
    }

    public static String username() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "system" : auth.getName();
    }

    public static Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof AdminUserDetails d ? d.getId() : null;
    }
}
