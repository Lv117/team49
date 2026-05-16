package cn.edu.sdu.java.server.configs;

import cn.edu.sdu.java.server.exception.BusinessException;
import cn.edu.sdu.java.server.exception.ErrorCodes;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returnsBusinessErrorCodeAsResponseField() {
        DataResponse response = handler.handleBusinessException(
                new BusinessException(ErrorCodes.STUDENT_NUM_CONFLICT, "学号已被占用，请使用其他学号"));

        assertEquals(1, response.getCode());
        assertEquals(ErrorCodes.STUDENT_NUM_CONFLICT, response.getErrorCode());
        assertEquals("学号已被占用，请使用其他学号", response.getMsg());
    }

    @Test
    void mapsGenericRuntimeExceptionToSystemErrorCode() {
        DataResponse response = handler.handleRuntimeException(new RuntimeException("数据库连接中断"));

        assertEquals(1, response.getCode());
        assertEquals(ErrorCodes.SYSTEM_ERROR, response.getErrorCode());
        assertEquals("业务错误：数据库连接中断", response.getMsg());
    }
}
