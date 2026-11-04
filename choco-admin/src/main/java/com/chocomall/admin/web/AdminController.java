package com.chocomall.admin.web;

import com.chocomall.admin.dto.AuthDtos.AdminInfo;
import com.chocomall.admin.dto.AuthDtos.AdminParam;
import com.chocomall.admin.dto.AuthDtos.AdminView;
import com.chocomall.admin.dto.AuthDtos.LoginParam;
import com.chocomall.admin.dto.AuthDtos.LoginResult;
import com.chocomall.admin.dto.AuthDtos.PasswordParam;
import com.chocomall.admin.dto.AuthDtos.RoleAssignParam;
import com.chocomall.admin.security.CurrentAdmin;
import com.chocomall.admin.service.AdminAccountService;
import com.chocomall.common.api.CommonPage;
import com.chocomall.common.api.CommonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Staff accounts")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminAccountService accounts;

    @Operation(summary = "Sign in and receive a JWT")
    @SecurityRequirements
    @PostMapping("/login")
    public CommonResult<LoginResult> login(@Valid @RequestBody LoginParam param, HttpServletRequest request) {
        return CommonResult.success(accounts.login(param, request.getRemoteAddr(), request.getHeader("User-Agent")));
    }

    @Operation(summary = "Profile, roles and menu tree of the signed-in staff member")
    @GetMapping("/info")
    public CommonResult<AdminInfo> info() {
        return CommonResult.success(accounts.info(CurrentAdmin.username()));
    }

    @Operation(summary = "Sign out (tokens are stateless; the client discards its token)")
    @PostMapping("/logout")
    public CommonResult<Void> logout() {
        return CommonResult.success();
    }

    @Operation(summary = "Change a password (own account needs the current password; super admins may reset others)")
    @PostMapping("/password")
    public CommonResult<Void> password(@Valid @RequestBody PasswordParam param, Authentication auth) {
        boolean isSuper = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("/admin/**"));
        accounts.changePassword(param, auth.getName(), isSuper);
        return CommonResult.success();
    }

    @GetMapping("/list")
    public CommonResult<CommonPage<AdminView>> list(@RequestParam(required = false) String keyword,
                                                    @RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(accounts.list(keyword, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public CommonResult<AdminView> get(@PathVariable Long id) {
        return CommonResult.success(accounts.get(id));
    }

    @Operation(summary = "Create a staff account")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResult<AdminView> create(@Valid @RequestBody AdminParam param) {
        return CommonResult.success(accounts.create(param));
    }

    @PostMapping("/update/{id}")
    public CommonResult<AdminView> update(@PathVariable Long id, @Valid @RequestBody AdminParam param) {
        return CommonResult.success(accounts.update(id, param));
    }

    @PostMapping("/updateStatus/{id}")
    public CommonResult<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        accounts.updateStatus(id, status, CurrentAdmin.username());
        return CommonResult.success();
    }

    @PostMapping("/delete/{id}")
    public CommonResult<Void> delete(@PathVariable Long id) {
        accounts.delete(id, CurrentAdmin.username());
        return CommonResult.success();
    }

    @Operation(summary = "Replace the roles of a staff account")
    @PostMapping("/role/update")
    public CommonResult<Void> assignRoles(@Valid @RequestBody RoleAssignParam param) {
        accounts.assignRoles(param.adminId(), param.roleIds());
        return CommonResult.success();
    }

    @GetMapping("/role/{adminId}")
    public CommonResult<Set<Long>> roles(@PathVariable Long adminId) {
        return CommonResult.success(accounts.roleIdsOf(adminId));
    }
}
