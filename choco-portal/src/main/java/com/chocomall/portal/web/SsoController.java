package com.chocomall.portal.web;

import com.chocomall.common.api.CommonResult;
import com.chocomall.portal.dto.PortalDtos;
import com.chocomall.portal.dto.PortalDtos.LoginParam;
import com.chocomall.portal.dto.PortalDtos.MemberProfile;
import com.chocomall.portal.dto.PortalDtos.ProfileParam;
import com.chocomall.portal.dto.PortalDtos.RegisterParam;
import com.chocomall.portal.dto.PortalDtos.ResetPasswordParam;
import com.chocomall.portal.dto.PortalDtos.TokenResult;
import com.chocomall.portal.security.CurrentMember;
import com.chocomall.portal.service.SsoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Account")
@Validated
@RestController
@RequestMapping("/sso")
@RequiredArgsConstructor
public class SsoController {

    private final SsoService sso;

    @Operation(summary = "Send a verification code to a phone number (one per minute). In development the code is returned.")
    @SecurityRequirements
    @GetMapping("/getAuthCode")
    public CommonResult<String> authCode(@RequestParam @Pattern(regexp = PortalDtos.PHONE, message = "must be a valid phone number") String telephone) {
        String code = sso.requestAuthCode(telephone);
        return code == null ? CommonResult.success(null, "Verification code sent") : CommonResult.success(code, "Verification code sent (development)");
    }

    @Operation(summary = "Create an account and sign in")
    @SecurityRequirements
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResult<TokenResult> register(@Valid @RequestBody RegisterParam param) {
        return CommonResult.success(sso.register(param));
    }

    @Operation(summary = "Sign in and receive a JWT")
    @SecurityRequirements
    @PostMapping("/login")
    public CommonResult<TokenResult> login(@Valid @RequestBody LoginParam param) {
        return CommonResult.success(sso.login(param));
    }

    @Operation(summary = "Reset a forgotten password with a phone verification code")
    @SecurityRequirements
    @PostMapping("/resetPassword")
    public CommonResult<Void> resetPassword(@Valid @RequestBody ResetPasswordParam param) {
        sso.resetPassword(param);
        return CommonResult.success();
    }

    @GetMapping("/info")
    public CommonResult<MemberProfile> info() {
        return CommonResult.success(sso.profile(CurrentMember.id()));
    }

    @PostMapping("/profile")
    public CommonResult<MemberProfile> updateProfile(@Valid @RequestBody ProfileParam param) {
        return CommonResult.success(sso.updateProfile(CurrentMember.id(), param));
    }
}
