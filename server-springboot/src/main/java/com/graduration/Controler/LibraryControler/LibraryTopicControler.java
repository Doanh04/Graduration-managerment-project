package com.graduration.Controler.LibraryControler;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/create-library-topic")
    public ApiResponse<LibraryTopicResponse> createLibraryTopic(@Valid @RequestBody LibraryTopicRequest request) {
        return ApiResponse.<LibraryTopicResponse>builder()
                .message("Library topic created successfully")
                .result(libraryTopicService.createLibraryTopic(request))
                .build();
    }

    @GetMapping("/{idLibraryTopic}")
    public ApiResponse<LibraryTopicResponse> getLibraryTopic(@PathVariable Long idLibraryTopic) {
        return ApiResponse.<LibraryTopicResponse>builder()
                .result(libraryTopicService.getLibraryTopic(idLibraryTopic))
                .build();
    }

    @GetMapping("/get-all-library-topic")
    public ApiResponse<List<LibraryTopicResponse>> getAllLibraryTopics(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer size) {
        return ApiResponse.<List<LibraryTopicResponse>>builder()
                .result(
                        page == null && size == null
                                ? libraryTopicService.getAllLibraryTopics()
                                : libraryTopicService.getAllLibraryTopics(page, size))
                .build();
    }

    @PutMapping("/{idLibraryTopic}")
    public ApiResponse<LibraryTopicResponse> updateLibraryTopic(
            @PathVariable Long idLibraryTopic, @Valid @RequestBody LibraryTopicRequest request) {
        return ApiResponse.<LibraryTopicResponse>builder()
                .message("Library topic updated successfully")
                .result(libraryTopicService.updateLibraryTopic(idLibraryTopic, request))
                .build();
    }

    @DeleteMapping("/{idLibraryTopic}")
    public ApiResponse<Void> deleteLibraryTopic(@PathVariable Long idLibraryTopic) {
        libraryTopicService.deleteLibraryTopic(idLibraryTopic);
        return ApiResponse.<Void>builder()
                .message("Library topic deleted successfully")
                .build();
    }
}
