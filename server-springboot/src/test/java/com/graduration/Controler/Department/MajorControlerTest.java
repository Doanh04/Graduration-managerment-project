package com.graduration.Controler.Department;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import com.graduration.DTO.Request.MajorRequest;
import com.graduration.DTO.Response.MajorResponse;
import com.graduration.Service.DerpatmentService.MajorService;
import com.graduration.exception.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class MajorControlerTest {
    MockMvc mockMvc;

    @Mock
    MajorService majorService;

    @InjectMocks
    MajorControler majorControler;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(majorControler)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createMajor_returnsCreatedMajor() throws Exception {
        when(majorService.createMajor(any(MajorRequest.class))).thenReturn(response());

        mockMvc.perform(
                        post("/major/create-major")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
								{
								"majorName": "Computer Science",
								"description": "Software and systems"
								}
								"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Major created successfully"))
                .andExpect(jsonPath("$.result.majorId").value(1))
                .andExpect(jsonPath("$.result.majorName").value("Computer Science"));

        verify(majorService).createMajor(any(MajorRequest.class));
    }

    @Test
    void getMajor_returnsMajor() throws Exception {
        when(majorService.getMajor(1L)).thenReturn(response());

        mockMvc.perform(get("/major/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.majorId").value(1))
                .andExpect(jsonPath("$.result.majorName").value("Computer Science"));

        verify(majorService).getMajor(1L);
    }

    @Test
    void getAllMajors_returnsMajorList() throws Exception {
        when(majorService.getAllMajorsPage(null, null))
                .thenReturn(com.graduration.DTO.Response.PageResponse.of(List.of(response())));

        mockMvc.perform(get("/major/get-all-major"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.content.length()").value(1))
                .andExpect(jsonPath("$.result.content[0].majorId").value(1))
                .andExpect(jsonPath("$.result.content[0].majorName").value("Computer Science"));

        verify(majorService).getAllMajorsPage(null, null);
    }

    @Test
    void updateMajor_returnsUpdatedMajor() throws Exception {
        MajorResponse updated = MajorResponse.builder()
                .majorId(1L)
                .majorName("Data Science")
                .description("Analytics")
                .build();
        when(majorService.updateMajor(eq(1L), any(MajorRequest.class))).thenReturn(updated);

        mockMvc.perform(
                        put("/major/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
								{
								"majorName": "Data Science",
								"description": "Analytics"
								}
								"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Major updated successfully"))
                .andExpect(jsonPath("$.result.majorId").value(1))
                .andExpect(jsonPath("$.result.majorName").value("Data Science"));

        verify(majorService).updateMajor(eq(1L), any(MajorRequest.class));
    }

    @Test
    void deleteMajor_returnsSuccessResponse() throws Exception {
        doNothing().when(majorService).deleteMajor(1L);

        mockMvc.perform(delete("/major/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Major deleted successfully"));

        verify(majorService).deleteMajor(1L);
    }

    private MajorResponse response() {
        return MajorResponse.builder()
                .majorId(1L)
                .majorName("Computer Science")
                .description("Software and systems")
                .build();
    }
}
