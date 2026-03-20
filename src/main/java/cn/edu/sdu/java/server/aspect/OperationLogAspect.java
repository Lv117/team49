package cn.edu.sdu.java.server.aspect;

import cn.edu.sdu.java.server.models.ModifyLog;
import cn.edu.sdu.java.server.repositorys.ModifyLogRepository;
import cn.edu.sdu.java.server.services.UserDetailsImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 操作日志切面
 * 用于记录用户的操作日志
 */
@Aspect
@Component
@Slf4j
public class OperationLogAspect {

    private final ModifyLogRepository modifyLogRepository;
    private final ObjectMapper objectMapper;

    public OperationLogAspect(ModifyLogRepository modifyLogRepository, ObjectMapper objectMapper) {
        this.modifyLogRepository = modifyLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 定义切点：所有 Controller 层的方法
     */
    @Before("execution(* cn.edu.sdu.java.server.controllers..*.*(..))")
    public void beforeMethod(JoinPoint joinPoint) {
        // 可以在这里记录请求开始时间
    }

    /**
     * 记录操作成功日志
     */
    @AfterReturning(pointcut = "execution(* cn.edu.sdu.java.server.controllers..*.*(..))", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        logOperation(joinPoint, result, null);
    }

    /**
     * 记录操作异常日志
     */
    @AfterThrowing(pointcut = "execution(* cn.edu.sdu.java.server.controllers..*.*(..))", throwing = "exception")
    public void afterThrowing(JoinPoint joinPoint, Exception exception) {
        logOperation(joinPoint, null, exception);
    }

    /**
     * 记录操作日志
     */
    private void logOperation(JoinPoint joinPoint, Object result, Exception exception) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String className = signature.getDeclaringType().getSimpleName();
            String methodName = signature.getName();
            
            // 获取当前用户
            String operatorName = getCurrentUsername();
            Integer operatorId = getCurrentUserId();
            
            // 构建日志信息
            ModifyLog modifyLog = new ModifyLog();
            modifyLog.setType(exception == null ? "INFO" : "ERROR");
            modifyLog.setTableName(className);
            
            String logInfo = String.format("方法：%s.%s | 操作人：%s | 结果：%s",
                className, methodName, operatorName, exception == null ? "成功" : "失败");
            
            if (exception != null) {
                logInfo += " | 异常：" + exception.getMessage();
            }
            
            modifyLog.setInfo(logInfo);
            modifyLog.setOperateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            modifyLog.setOperatorId(operatorId);
            
            modifyLogRepository.save(modifyLog);
            
        } catch (Exception e) {
            log.error("记录操作日志失败：{}", e.getMessage());
        }
    }

    /**
     * 获取当前登录用户名
     */
    private String getCurrentUsername() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetailsImpl) {
                return ((UserDetailsImpl) principal).getUsername();
            }
        } catch (Exception e) {
            // 未登录或获取失败
        }
        return "anonymous";
    }

    /**
     * 获取当前登录用户 ID
     */
    private Integer getCurrentUserId() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetailsImpl) {
                return ((UserDetailsImpl) principal).getId();
            }
        } catch (Exception e) {
            // 未登录或获取失败
        }
        return null;
    }
}
