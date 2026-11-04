package com.chocomall.portal.security;

import com.chocomall.common.api.ResultCode;
import com.chocomall.common.exception.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** The signed-in customer. Public endpoints use {@link #idOrNull()} to personalise anonymously-readable pages. */
public final class CurrentMember {

    private CurrentMember() {
    }

    public static Long id() {
        Long id = idOrNull();
        if (id == null) {
            throw new ApiException(ResultCode.UNAUTHORIZED);
        }
        return id;
    }

    public static Long idOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof MemberUserDetails d ? d.getId() : null;
    }

    public static String username() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof MemberUserDetails d ? d.getUsername() : "anonymous";
    }
}
