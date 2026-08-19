package com.graduration.Controler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.graduration.Constain.CategoryTopicConstain;
import com.graduration.Constain.TopicStatusConstain;
import com.graduration.Controler.GradurationControler.TopicControler;
import com.graduration.DTO.Request.CreateTopicRequest;
import com.graduration.DTO.Request.UpdateTopicRequest;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.DTO.Response.TopicResponse;
import com.graduration.Service.GradurationService.TopicService;
import com.graduration.exception.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class TopicControlerTest {
    MockMvc mockMvc;

    @Mock
    TopicService topicService;

    @InjectMocks
    TopicControler topicControler;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(topicControler)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createTopic_keepsCategoryAndDefensePeriod() throws Exception {
        when(topicService.createTopic(any(CreateTopicRequest.class))).thenReturn(response());

        mockMvc.perform(post("/topics").contentType(MediaType.APPLICATION_JSON).content(validJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.categoryTopic").value("STUDENT"))
                .andExpect(jsonPath("$.result.defensePeriodId").value(10))
                .andExpect(jsonPath("$.result.academicYearId").value(1));
    }

    @Test
    void createTopic_requiresCategoryTopic() throws Exception {
        mockMvc.perform(post("/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson().replace("\"categoryTopic\":\"STUDENT\",", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTopics_forwardsPaginationAndFilters() throws Exception {
        when(topicService.getTopics(1, 20, 1, 10L, CategoryTopicConstain.STUDENT, TopicStatusConstain.APPROVED, "java"))
                .thenReturn(PageResponse.of(List.of(response())));

        mockMvc.perform(get("/topics")
                        .param("page", "1")
                        .param("size", "20")
                        .param("academicYearId", "1")
                        .param("defensePeriodId", "10")
                        .param("categoryTopic", "STUDENT")
                        .param("status", "APPROVED")
                        .param("keyword", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].topicId").value(5));
    }

    @Test
    void getTopic_returnsTopic() throws Exception {
        when(topicService.getTopic(5L)).thenReturn(response());
        mockMvc.perform(get("/topics/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.title").value("Graduation topic"));
    }

    @Test
    void updateTopic_returnsUpdatedTopic() throws Exception {
        when(topicService.updateTopic(eq(5L), any(UpdateTopicRequest.class))).thenReturn(response());
        mockMvc.perform(patch("/topics/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Topic updated successfully"));
    }

    @Test
    void lifecycleEndpoints_returnExpectedMessages() throws Exception {
        when(topicService.submitForApproval(5L)).thenReturn(response());
        when(topicService.approveTopic(5L)).thenReturn(response());
        when(topicService.rejectTopic(5L, "Needs details")).thenReturn(response());

        mockMvc.perform(post("/topics/5/submit-for-approval")).andExpect(status().isOk());
        mockMvc.perform(post("/topics/5/approve")).andExpect(status().isOk());
        mockMvc.perform(post("/topics/5/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Needs details\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteTopic_callsService() throws Exception {
        mockMvc.perform(delete("/topics/5")).andExpect(status().isOk());
        verify(topicService).deleteTopic(5L);
    }

    private String validJson() {
        return """
				{
				"title":"Graduation topic",
				"description":"Description",
				"objective":"Objective",
				"technology":"Java",
				"categoryTopic":"STUDENT",
				"defensePeriodId":10
				}
				""";
    }

    private TopicResponse response() {
        return TopicResponse.builder()
                .topicId(5L)
                .title("Graduation topic")
                .categoryTopic(CategoryTopicConstain.STUDENT)
                .status(TopicStatusConstain.DRAFT)
                .defensePeriodId(10L)
                .academicYearId(1)
                .academicYear("2025-2026")
                .build();
    }
}
