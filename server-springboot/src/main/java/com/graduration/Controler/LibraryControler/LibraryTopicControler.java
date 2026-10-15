package com.graduration.Controler.LibraryControler;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.graduration.DTO.Request.LibraryTopicRequest;
import com.graduration.DTO.Response.ApiResponse;
import com.graduration.DTO.Response.LibraryTopicResponse;
import com.graduration.Service.LibraryService.LibraryTopicService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/library-topic")
@RequiredArgsConstructor
public class LibraryTopicControler {
    private final LibraryTopicService libraryTopicService;

    @PostMapping(value = "/create-library-topic", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<LibraryTopicResponse> createLibraryTopic(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String objective,
            @RequestParam(required = false) String technology,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ApiResponse.<LibraryTopicResponse>builder()
                .message("Thêm đề tài tham khảo thành công")
                .result(libraryTopicService.createLibraryTopic(
                        new LibraryTopicRequest(title, description, objective, technology), file))
                .build();
    }

    @GetMapping("/{idLibraryTopic}")
    public ApiResponse<LibraryTopicResponse> getLibraryTopic(@PathVariable Long idLibraryTopic) {
        return ApiResponse.<LibraryTopicResponse>builder()
                .result(libraryTopicService.getLibraryTopic(idLibraryTopic))
                .build();
    }

    @GetMapping("/get-all-library-topic")
    public ApiResponse<com.graduration.DTO.Response.PageResponse<LibraryTopicResponse>> getAllLibraryTopics(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer size) {
        return ApiResponse.<com.graduration.DTO.Response.PageResponse<LibraryTopicResponse>>builder()
                .result(libraryTopicService.getAllLibraryTopicsPage(page, size))
                .build();
    }

    @PutMapping(value = "/{idLibraryTopic}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<LibraryTopicResponse> updateLibraryTopic(
            @PathVariable Long idLibraryTopic,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String objective,
            @RequestParam(required = false) String technology,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ApiResponse.<LibraryTopicResponse>builder()
                .message("Cập nhật đề tài tham khảo thành công")
                .result(libraryTopicService.updateLibraryTopic(
                        idLibraryTopic, new LibraryTopicRequest(title, description, objective, technology), file))
                .build();
    }

    @GetMapping("/{idLibraryTopic}/file")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long idLibraryTopic) {
        LibraryTopicService.DownloadedLibraryTopic file = libraryTopicService.downloadFile(idLibraryTopic);
        MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            if (file.contentType() != null) {
                contentType = MediaType.parseMediaType(file.contentType());
            }
        } catch (IllegalArgumentException ignored) {
            // Metadata cũ không hợp lệ vẫn được trả về dưới dạng dữ liệu nhị phân.
        }
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.resource());
    }

    @DeleteMapping("/{idLibraryTopic}")
    public ApiResponse<Void> deleteLibraryTopic(@PathVariable Long idLibraryTopic) {
        libraryTopicService.deleteLibraryTopic(idLibraryTopic);
        return ApiResponse.<Void>builder()
                .message("Xóa đề tài tham khảo thành công")
                .build();
    }
}
