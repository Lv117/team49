package cn.edu.sdu.java.server.configs;

import cn.edu.sdu.java.server.payload.response.DataResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public DataResponse handleException(Exception e) {
        e.printStackTrace();
        return DataResponse.error("服务器内部错误：" + e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public DataResponse handleIllegalArgumentException(IllegalArgumentException e) {
        return DataResponse.error("参数错误：" + e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public DataResponse handleRuntimeException(RuntimeException e) {
        return DataResponse.error("业务错误：" + e.getMessage());
    }
}
