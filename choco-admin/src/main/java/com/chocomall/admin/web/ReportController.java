package com.chocomall.admin.web;

import com.chocomall.admin.service.ReportService;
import com.chocomall.common.api.CommonResult;
import com.chocomall.core.storage.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Reports and files")
@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reports;
    private final FileStorageService files;

    @Operation(summary = "Today's revenue and orders, the order queue and items that need attention")
    @GetMapping("/report/dashboard")
    public CommonResult<Map<String, Object>> dashboard() {
        return CommonResult.success(reports.dashboard());
    }

    @GetMapping("/report/sales/daily")
    public CommonResult<List<Map<String, Object>>> daily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return CommonResult.success(reports.dailySales(from, to));
    }

    @GetMapping("/report/products/top")
    public CommonResult<List<Map<String, Object>>> top(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit) {
        return CommonResult.success(reports.topProducts(from, to, limit));
    }

    @Operation(summary = "Per-product contribution after ingredient, packaging, labour and selling fees")
    @GetMapping("/report/contribution")
    public CommonResult<List<Map<String, Object>>> contribution(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return CommonResult.success(reports.contribution(from, to));
    }

    @Operation(summary = "Upload an image (PNG, JPEG, WebP or GIF, up to 5 MB) and receive its URL")
    @PostMapping("/file/upload")
    public CommonResult<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        return CommonResult.success(Map.of("url", files.store(file)));
    }
}
