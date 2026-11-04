package com.chocomall.portal.web;

import com.chocomall.common.api.CommonResult;
import com.chocomall.portal.service.ProductActivityService;
import com.chocomall.portal.service.ProductActivityService.Activity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@SecurityRequirements
public class ProductActivityController {
    private final ProductActivityService activity;

    @GetMapping("/product/{id}/activity")
    @Operation(summary = "Anonymous aggregate counts: today's browser visitors, current basket accounts, this month's paid buyers")
    public CommonResult<Activity> snapshot(@PathVariable Long id, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return CommonResult.success(activity.snapshot(id));
    }

    @PostMapping("/product/{id}/activity/view")
    @Operation(summary = "Record one unique browser view per product per shop day, then return activity counts")
    public CommonResult<Activity> view(@PathVariable Long id,
            @CookieValue(name = "choco_visitor", required = false) String visitor,
            HttpServletRequest request, HttpServletResponse response) {
        boolean newVisitor = visitor == null || !visitor.matches("[a-f0-9]{32}");
        if (newVisitor) visitor = UUID.randomUUID().toString().replace("-", "");
        Activity result = activity.recordView(id, visitor);
        if (newVisitor) response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("choco_visitor", visitor)
                .httpOnly(true).secure(request.isSecure()).sameSite("Lax").path("/product")
                .maxAge(Duration.ofDays(31)).build().toString());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return CommonResult.success(result);
    }
}
