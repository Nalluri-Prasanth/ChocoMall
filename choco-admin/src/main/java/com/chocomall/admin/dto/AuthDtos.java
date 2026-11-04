package com.chocomall.admin.dto;

import com.chocomall.model.entity.AdminUser;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** Request and response shapes for authentication and staff management. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginParam(@NotBlank String username, @NotBlank String password) {
    }

    public record LoginResult(String token, String tokenHead, long expiresInSeconds) {
    }

    public record MenuNode(Long id, Long parentId, String title, Integer level, Integer sort, String name, String icon,
                           boolean hidden, List<MenuNode> children) {
    }

    public record AdminInfo(Long id, String username, String nickName, String icon, List<String> roles, List<MenuNode> menus) {
    }

    public record AdminParam(
            @NotBlank @Size(min = 3, max = 64) @Pattern(regexp = "[A-Za-z0-9._-]+", message = "may only contain letters, digits, dots, underscores and hyphens") String username,
            @Size(min = 8, max = 72) String password,
            @Size(max = 200) String nickName,
            @Email @Size(max = 100) String email,
            @Size(max = 500) String icon,
            @Size(max = 500) String note,
            Integer status) {
    }

    public record PasswordParam(@NotBlank String username, String oldPassword,
                                @NotBlank @Size(min = 8, max = 72) String newPassword) {
    }

    public record RoleAssignParam(@NotNull Long adminId, @NotNull Set<Long> roleIds) {
    }

    public record IdsParam(@NotEmpty Set<Long> ids) {
    }

    public record AdminView(Long id, String username, String nickName, String email, String icon, String note,
                            LocalDateTime createTime, LocalDateTime loginTime, Integer status, Set<Long> roleIds) {

        public static AdminView from(AdminUser a) {
            return new AdminView(a.getId(), a.getUsername(), a.getNickName(), a.getEmail(), a.getIcon(), a.getNote(),
                    a.getCreateTime(), a.getLoginTime(), a.getStatus(), a.getRoleIds());
        }
    }

    public record RoleParam(@NotBlank @Size(max = 100) String name, @Size(max = 500) String description,
                            Integer sort) {
    }

    public record MenuParam(Long parentId, @NotBlank @Size(max = 100) String title, @Size(max = 100) String name,
                            @Size(max = 200) String icon, Integer sort, Boolean hidden) {
    }

    public record ResourceParam(Long categoryId, @NotBlank @Size(max = 200) String name,
                                @NotBlank @Size(max = 200) String url, @Size(max = 500) String description) {
    }

    public record CategoryParam(@NotBlank @Size(max = 200) String name, Integer sort) {
    }

    public record AllocParam(@NotNull Long roleId, @NotNull Set<Long> ids) {
    }
}
