package cn.edu.sdu.java.server.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import cn.edu.sdu.java.server.payload.request.LoginRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 兼容旧版前端路径的转发控制器
 * 将 /auth/login 转发到 /api/auth/login
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/auth")
@Tag(name = "Legacy Authentication", description = "Legacy login compatibility APIs")
public class LegacyAuthController {

    private final AuthService authService;
    
    public LegacyAuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "l og in")
    @PostMapping("/login")
    public DataResponse login(@Valid @RequestBody LoginRequest loginRequest) {
        return authService.authenticateUser(loginRequest);
    }
}
