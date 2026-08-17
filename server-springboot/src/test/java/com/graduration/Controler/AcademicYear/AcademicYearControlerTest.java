package com.graduration.Controler.AcademicYear;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.graduration.DTO.Request.AcademicYearRequest;
import com.graduration.DTO.Response.AcademicYearResponse;
import com.graduration.Service.AcademicService.AcademicYearService;
import com.graduration.exception.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class AcademicYearControlerTest {
    MockMvc mockMvc;

    @Mock
    AcademicYearService academicYearService;

    @InjectMocks
    AcademicYearControler academicYearControler;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(academicYearControler)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createAcademicYear_returnsCreatedYear() throws Exception {
        when(academicYearService.createAcademicYear(any(AcademicYearRequest.class)))
                .thenReturn(response());

        mockMvc.perform(post("/academic-year/create-academic-year")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"academicYear\":\"2025-2026\",\"description\":\"Year 2025\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Academic year created successfully"))
                .andExpect(jsonPath("$.result.academicYear").value("2025-2026"));
    }

    @Test
    void createAcademicYear_rejectsInvalidFormat() throws Exception {
        mockMvc.perform(post("/academic-year/create-academic-year")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"academicYear\":\"2025\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAcademicYear_returnsYear() throws Exception {
        when(academicYearService.getAcademicYear(1)).thenReturn(response());

        mockMvc.perform(get("/academic-year/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.academicId").value(1));
    }

    @Test
    void getAcademicYearByName_returnsYear() throws Exception {
        when(academicYearService.getAcademicYearByName("2025-2026")).thenReturn(response());

        mockMvc.perform(get("/academic-year/name/2025-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.academicYear").value("2025-2026"));
    }

    @Test
    void getAllAcademicYears_returnsList() throws Exception {
        when(academicYearService.getAllAcademicYearsPage(null, null))
                .thenReturn(com.graduration.DTO.Response.PageResponse.of(List.of(response())));

        mockMvc.perform(get("/academic-year/get-all-academic-year"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content.length()").value(1));
    }

    @Test
    void updateAcademicYear_returnsUpdatedYear() throws Exception {
        when(academicYearService.updateAcademicYear(eq(1), any(AcademicYearRequest.class)))
                .thenReturn(response());

        mockMvc.perform(put("/academic-year/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"academicYear\":\"2025-2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Academic year updated successfully"));
    }

    @Test
    void deleteAcademicYear_returnsSuccess() throws Exception {
        mockMvc.perform(delete("/academic-year/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Academic year deleted successfully"));

        verify(academicYearService).deleteAcademicYear(1);
    }

    private AcademicYearResponse response() {
        return AcademicYearResponse.builder()
                .academicId(1)
                .academicYear("2025-2026")
                .build();
    }
}
