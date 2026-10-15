package com.graduration.Controler.ManagerControler;

import java.util.List;
import java.nio.charset.StandardCharsets;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.graduration.DTO.Response.ApiResponse;
import com.graduration.Service.ManagerService.DefenseReportService;
import com.graduration.Service.ManagerService.DefenseReportService.*;
import lombok.RequiredArgsConstructor;

@RestController @RequestMapping("/defense-reports") @RequiredArgsConstructor
public class DefenseReportController {
    private final DefenseReportService reports;
    @GetMapping("/mine")
    public ApiResponse<List<Overview>> mine() { return ApiResponse.<List<Overview>>builder().result(reports.mine()).build(); }
    @GetMapping("/assignment/{id}")
    public ApiResponse<Overview> get(@PathVariable Long id) { return ApiResponse.<Overview>builder().result(reports.get(id)).build(); }
    @PostMapping(value = "/assignment/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Overview> submit(@PathVariable Long id, @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String sourceUrl, @RequestParam(required = false) String productUrl,
            @RequestParam(required = false) String note) {
        return ApiResponse.<Overview>builder().result(reports.submit(id, file, sourceUrl, productUrl, note)).build();
    }
    @PostMapping("/assignment/{id}/review")
    public ApiResponse<Overview> review(@PathVariable Long id, @RequestBody Decision decision) {
        return ApiResponse.<Overview>builder().result(reports.review(id, decision)).build();
    }
    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> file(@PathVariable Long id) {
        var file = reports.file(id);
        var disposition = ("application/pdf".equals(file.type()) ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(file.name(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.type()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store").header("X-Content-Type-Options", "nosniff").body(file.bytes());
    }
}
