package cn.edu.sdu.java.server.controllers;

import cn.edu.sdu.java.server.configs.GlobalExceptionHandler;
import cn.edu.sdu.java.server.exception.BusinessException;
import cn.edu.sdu.java.server.exception.ErrorCodes;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.ConsumptionService;
import cn.edu.sdu.java.server.services.CourseMaterialService;
import cn.edu.sdu.java.server.services.ResumeService;
import cn.edu.sdu.java.server.services.ScoreCalculationService;
import cn.edu.sdu.java.server.services.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class StudentControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentService studentService;
    @MockBean
    private CourseMaterialService courseMaterialService;
    @MockBean
    private ConsumptionService consumptionService;
    @MockBean
    private ScoreCalculationService scoreCalculationService;
    @MockBean
    private ResumeService resumeService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void returnsSuccessWhenStudentIsAdded() throws Exception {
        when(studentService.studentEditSave(any()))
                .thenReturn(new DataResponse(0, 101, "success"));

        mockMvc.perform(post("/api/student/studentEditSave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "data": {
                                    "form": {
                                      "num": "20260001",
                                      "name": "张三"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(101));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void returnsConflictCodeWhenStudentNumberDuplicated() throws Exception {
        when(studentService.studentEditSave(any()))
                .thenReturn(DataResponse.error("学号已被占用，请使用其他学号", ErrorCodes.STUDENT_NUM_CONFLICT));

        mockMvc.perform(post("/api/student/studentEditSave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "data": {
                                    "form": {
                                      "num": "20260001"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value(ErrorCodes.STUDENT_NUM_CONFLICT))
                .andExpect(jsonPath("$.msg").value("学号已被占用，请使用其他学号"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void returnsRequiredCodeWhenStudentNumberMissing() throws Exception {
        when(studentService.studentEditSave(any()))
                .thenReturn(DataResponse.error("学号不能为空", ErrorCodes.STUDENT_NUM_REQUIRED));

        mockMvc.perform(post("/api/student/studentEditSave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "data": {
                                    "form": {
                                      "num": "   "
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value(ErrorCodes.STUDENT_NUM_REQUIRED))
                .andExpect(jsonPath("$.msg").value("学号不能为空"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void mapsDatabaseExceptionToPreciseErrorCode() throws Exception {
        when(studentService.studentEditSave(any()))
                .thenThrow(new BusinessException(ErrorCodes.STUDENT_SAVE_TIMEOUT, "数据库写入超时，请稍后重试"));

        mockMvc.perform(post("/api/student/studentEditSave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "data": {
                                    "form": {
                                      "num": "20260009"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value(ErrorCodes.STUDENT_SAVE_TIMEOUT))
                .andExpect(jsonPath("$.msg").value("数据库写入超时，请稍后重试"));
    }
}
