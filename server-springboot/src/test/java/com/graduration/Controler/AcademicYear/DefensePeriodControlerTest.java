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

import com.graduration.Constain.DefensePeriodConstain;
import com.graduration.DTO.Request.DefensePeriodRequest;
import com.graduration.DTO.Response.DefensePeriodResponse;
import com.graduration.Service.AcademicService.DefensePeriodService;
import com.graduration.exception.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class DefensePeriodControlerTest {
    MockMvc mockMvc;

    @Mock
    DefensePeriodService defensePeriodService;

    @InjectMocks
    DefensePeriodControler defensePeriodControler;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(defensePeriodControler)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createDefensePeriod_returnsCreatedPeriod() throws Exception {
        when(defensePeriodService.createDefensePeriod(any(DefensePeriodRequest.class)))
                .thenReturn(response());

        mockMvc.perform(post("/defense-period/create-defense-period")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Defense period created successfully"))
                .andExpect(jsonPath("$.result.status").value("PENDING"));
    }

    @Test
    void createDefensePeriod_rejectsUnknownStatus() throws Exception {
        mockMvc.perform(post("/defense-period/create-defense-period")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson().replace("PENDING", "INVALID")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getDefensePeriod_returnsPeriod() throws Exception {
        when(defensePeriodService.getDefensePeriod(10L)).thenReturn(response());

        mockMvc.perform(get("/defense-period/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.defensePeriodId").value(10));
    }

    @Test
    void getAllDefensePeriods_returnsList() throws Exception {
        when(defensePeriodService.getAllDefensePeriods()).thenReturn(List.of(response()));

        mockMvc.perform(get("/defense-period/get-all-defense-period"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(1));
    }

    @Test
    void getDefensePeriodsByAcademicYear_returnsList() throws Exception {
        when(defensePeriodService.getDefensePeriodsByAcademicYear(1)).thenReturn(List.of(response()));

        mockMvc.perform(get("/defense-period/academic-year/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].academicId").value(1));
    }

    @Test
    void updateDefensePeriod_returnsUpdatedPeriod() throws Exception {
        when(defensePeriodService.updateDefensePeriod(eq(10L), any(DefensePeriodRequest.class)))
                .thenReturn(response());

        mockMvc.perform(put("/defense-period/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Defense period updated successfully"));
    }

    @Test
    void deleteDefensePeriod_returnsSuccess() throws Exception {
        mockMvc.perform(delete("/defense-period/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Defense period deleted successfully"));

        verify(defensePeriodService).deleteDefensePeriod(10L);
    }

    private String validJson() {
        return """
				{
				"periodName":"Period 1",
				"startDate":"2026-09-01",
				"endDate":"2026-12-31",
				"projectType":"Graduation",
				"status":"PENDING",
				"academicId":1
				}
				""";
    }

    private DefensePeriodResponse response() {
        return DefensePeriodResponse.builder()
                .defensePeriodId(10L)
                .periodName("Period 1")
                .status(DefensePeriodConstain.PENDING)
                .academicId(1)
                .academicYear("2025-2026")
                .build();
    }
}
