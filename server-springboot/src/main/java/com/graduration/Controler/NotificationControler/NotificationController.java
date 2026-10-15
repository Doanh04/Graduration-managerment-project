package com.graduration.Controler.NotificationControler;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.graduration.DTO.Response.ApiResponse;
import com.graduration.DTO.Response.NotificationResponse;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.Service.NotificationService.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> getMine(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.<PageResponse<NotificationResponse>>builder()
                .result(notificationService.getMine(page, size))
                .build();
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount() {
        return ApiResponse.<Long>builder().result(notificationService.unreadCount()).build();
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable String notificationId) {
        return ApiResponse.<NotificationResponse>builder()
                .result(notificationService.markRead(notificationId))
                .build();
    }

    @PatchMapping("/read-all")
    public ApiResponse<Long> markAllRead() {
        return ApiResponse.<Long>builder()
                .message("Đã đánh dấu tất cả thông báo là đã đọc")
                .result(notificationService.markAllRead())
                .build();
    }
}
