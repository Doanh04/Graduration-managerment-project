package com.graduration.Controler.GradurationControler;

import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.graduration.Constain.CategoryTopicConstain;
import com.graduration.Constain.TopicStatusConstain;
import com.graduration.DTO.Request.CreateSupervisorTopicProposalRequest;
import com.graduration.DTO.Request.CreateTopicRequest;
import com.graduration.DTO.Request.RejectTopicRequest;
import com.graduration.DTO.Request.UpdateTopicRequest;
import com.graduration.DTO.Response.ApiResponse;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.DTO.Response.TopicResponse;
import com.graduration.Service.GradurationService.TopicService;
import com.graduration.Service.GradurationService.TopicService.ImportTopicResult;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/topics")
@RequiredArgsConstructor
public class TopicControler {
    private final TopicService topicService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<TopicResponse> createTopic(@Valid @RequestBody CreateTopicRequest request) {
        return ApiResponse.<TopicResponse>builder()
                .message("Topic created successfully")
                .result(topicService.createTopic(request))
                .build();
    }

    /** Nhận đề xuất đề tài của giảng viên kèm nhóm sinh viên để đưa vào hàng chờ admin xét duyệt. */
    @PostMapping(value = "/supervisor-proposals", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<TopicResponse> createSupervisorProposal(
            @Valid @RequestBody CreateSupervisorTopicProposalRequest request) {
        return ApiResponse.<TopicResponse>builder()
                .message("Supervisor topic proposal submitted successfully")
                .result(topicService.createSupervisorProposal(request))
                .build();
    }

    // Chấp nhận cả multipart có tham số charset do một số trình duyệt/HTTP client tự thêm.
    @PostMapping(
            value = "/supervisor-proposals",
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE + ";charset=UTF-8"})
    public ApiResponse<TopicResponse> createSupervisorProposalMultipart(
            @RequestParam(name = "title") String title,
            @RequestParam(name = "description", required = false) String description,
            @RequestParam(name = "objective", required = false) String objective,
            @RequestParam(name = "technology", required = false) String technology,
            @RequestParam(name = "defensePeriodId") Long defensePeriodId,
            @RequestParam(name = "teamName") String teamName,
            @RequestParam(name = "studentCodes") List<String> studentCodes,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        CreateSupervisorTopicProposalRequest request = CreateSupervisorTopicProposalRequest.builder()
                .title(title)
                .description(description)
                .objective(objective)
                .technology(technology)
                .defensePeriodId(defensePeriodId)
                .teamName(teamName)
                .studentCodes(studentCodes)
                .build();
        return ApiResponse.<TopicResponse>builder()
                .message("Supervisor topic proposal submitted successfully")
                .result(topicService.createSupervisorProposal(request, file))
                .build();
    }

    @PostMapping(
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE + ";charset=UTF-8"})
    public ApiResponse<TopicResponse> createTopicMultipart(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String objective,
            @RequestParam(required = false) String technology,
            @RequestParam CategoryTopicConstain categoryTopic,
            @RequestParam Long defensePeriodId,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        CreateTopicRequest request = CreateTopicRequest.builder()
                .title(title)
                .description(description)
                .objective(objective)
                .technology(technology)
                .categoryTopic(categoryTopic)
                .defensePeriodId(defensePeriodId)
                .build();
        return ApiResponse.<TopicResponse>builder()
                .message("Topic created successfully")
                .result(topicService.createTopic(request, file))
                .build();
    }

    @PostMapping(
            value = "/import",
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE + ";charset=UTF-8"})
    public ApiResponse<ImportTopicResult> importTopics(@RequestPart("file") MultipartFile file) {
        return ApiResponse.<ImportTopicResult>builder()
                .message("Topics imported successfully")
                .result(topicService.importTopics(file))
                .build();
    }

    @GetMapping("/eligible-for-defense-schedule")
    public ApiResponse<List<TopicResponse>> getEligibleForDefenseSchedule(@RequestParam Long defensePeriodId) {
        return ApiResponse.<List<TopicResponse>>builder()
                .result(topicService.getEligibleForDefenseSchedule(defensePeriodId))
                .build();
    }

    @GetMapping("/{topicId}")
    public ApiResponse<TopicResponse> getTopic(@PathVariable Long topicId) {
        return ApiResponse.<TopicResponse>builder()
                .result(topicService.getTopic(topicId))
                .build();
    }

    @GetMapping("/{topicId}/file")
    public ResponseEntity<Resource> downloadTopicFile(@PathVariable Long topicId) {
        TopicService.DownloadedTopic file = topicService.downloadTopicFile(topicId);
        MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            if (file.contentType() != null) {
                contentType = MediaType.parseMediaType(file.contentType());
            }
        } catch (IllegalArgumentException ignored) {
            // Trả về kiểu nhị phân khi metadata content type không hợp lệ.
        }
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.resource());
    }

    @GetMapping("/my-proposals")
    public ApiResponse<List<TopicResponse>> getMyProposals() {
        return ApiResponse.<List<TopicResponse>>builder()
                .result(topicService.getMyProposals())
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<TopicResponse>> getTopics(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer academicYearId,
            @RequestParam(required = false) Long defensePeriodId,
            @RequestParam(required = false) CategoryTopicConstain categoryTopic,
            @RequestParam(required = false) TopicStatusConstain status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean excludeStudentProposals) {
        return ApiResponse.<PageResponse<TopicResponse>>builder()
                .result(topicService.getTopics(
                        page,
                        size,
                        academicYearId,
                        defensePeriodId,
                        categoryTopic,
                        status,
                        keyword,
                        excludeStudentProposals))
                .build();
    }

    @PatchMapping(value = "/{topicId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<TopicResponse> updateTopic(
            @PathVariable Long topicId, @Valid @RequestBody UpdateTopicRequest request) {
        return ApiResponse.<TopicResponse>builder()
                .message("Topic updated successfully")
                .result(topicService.updateTopic(topicId, request))
                .build();
    }

    @PatchMapping(
            value = "/{topicId}",
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE + ";charset=UTF-8"})
    public ApiResponse<TopicResponse> updateTopicMultipart(
            @PathVariable Long topicId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String objective,
            @RequestParam(required = false) String technology,
            @RequestParam CategoryTopicConstain categoryTopic,
            @RequestParam Long defensePeriodId,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        UpdateTopicRequest request = UpdateTopicRequest.builder()
                .title(title)
                .description(description)
                .objective(objective)
                .technology(technology)
                .categoryTopic(categoryTopic)
                .defensePeriodId(defensePeriodId)
                .build();
        return ApiResponse.<TopicResponse>builder()
                .message("Topic updated successfully")
                .result(topicService.updateTopic(topicId, request, file))
                .build();
    }

    @DeleteMapping("/{topicId}")
    public ApiResponse<Void> deleteTopic(@PathVariable Long topicId) {
        topicService.deleteTopic(topicId);
        return ApiResponse.<Void>builder().message("Topic deleted successfully").build();
    }

    @PostMapping("/{topicId}/submit-for-approval")
    public ApiResponse<TopicResponse> submitForApproval(@PathVariable Long topicId) {
        return ApiResponse.<TopicResponse>builder()
                .message("Topic submitted for approval")
                .result(topicService.submitForApproval(topicId))
                .build();
    }

    @PostMapping("/{topicId}/approve")
    public ApiResponse<TopicResponse> approveTopic(@PathVariable Long topicId) {
        return ApiResponse.<TopicResponse>builder()
                .message("Topic approved successfully")
                .result(topicService.approveTopic(topicId))
                .build();
    }

    @PostMapping("/{topicId}/reject")
    public ApiResponse<TopicResponse> rejectTopic(
            @PathVariable Long topicId, @Valid @RequestBody RejectTopicRequest request) {
        return ApiResponse.<TopicResponse>builder()
                .message("Topic rejected successfully")
                .result(topicService.rejectTopic(topicId, request.getReason()))
                .build();
    }
}
