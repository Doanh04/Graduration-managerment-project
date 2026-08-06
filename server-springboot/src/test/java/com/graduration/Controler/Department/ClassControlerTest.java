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

import com.graduration.DTO.Request.ClassRequest;
import com.graduration.DTO.Response.ClassResponse;
import com.graduration.Service.DerpatmentService.ClassService;
import com.graduration.exception.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class ClassControlerTest {
    MockMvc mockMvc;

    @Mock
    ClassService classService;

    @InjectMocks
    ClassControler classControler;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(classControler)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createClass_returnsCreatedClass() throws Exception {
        when(classService.createClass(any(ClassRequest.class))).thenReturn(response());

        mockMvc.perform(post("/class/create-class")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Class created successfully"))
                .andExpect(jsonPath("$.result.idClass").value(10))
                .andExpect(jsonPath("$.result.classCode").value("CNTT01"));

        verify(classService).createClass(any(ClassRequest.class));
    }

    @Test
    void getClass_returnsClass() throws Exception {
        when(classService.getClass("CNTT01")).thenReturn(response());

        mockMvc.perform(get("/class/CNTT01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.idClass").value(10))
                .andExpect(jsonPath("$.result.className").value("Information Technology 1"));

        verify(classService).getClass("CNTT01");
    }

    @Test
    void getAllClasses_returnsClassList() throws Exception {
        when(classService.getAllClasses()).thenReturn(List.of(response()));

        mockMvc.perform(get("/class/get-all-class"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.length()").value(1))
                .andExpect(jsonPath("$.result[0].idClass").value(10));

        verify(classService).getAllClasses();
    }

    @Test
    void updateClass_returnsUpdatedClass() throws Exception {
        ClassResponse updated = ClassResponse.builder()
                .idClass(10L)
                .classCode("CNTT02")
                .className("Information Technology 2")
                .description("Updated class")
                .build();
        when(classService.updateClass(eq(10L), any(ClassRequest.class))).thenReturn(updated);

        mockMvc.perform(
                        put("/class/10")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
								{
								"classCode": "CNTT02",
								"nameClass": "Information Technology 2",
								"majorId": "1",
								"description": "Updated class"
								}
								"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Class updated successfully"))
                .andExpect(jsonPath("$.result.idClass").value(10))
                .andExpect(jsonPath("$.result.classCode").value("CNTT02"));

        verify(classService).updateClass(eq(10L), any(ClassRequest.class));
    }

    @Test
    void deleteClass_returnsSuccessResponse() throws Exception {
        doNothing().when(classService).deleteClass(10L);

        mockMvc.perform(delete("/class/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Class deleted successfully"));

        verify(classService).deleteClass(10L);
    }

    private String requestJson() {
        return """
				{
				"classCode": "CNTT01",
				"nameClass": "Information Technology 1",
				"majorId": "1",
				"description": "First class"
				}
				""";
    }

    private ClassResponse response() {
        return ClassResponse.builder()
                .idClass(10L)
                .classCode("CNTT01")
                .className("Information Technology 1")
                .description("First class")
                .build();
    }
}
