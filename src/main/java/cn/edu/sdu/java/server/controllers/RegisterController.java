package cn.edu.sdu.java.server.controllers;

import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.RegisterService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户注册控制器
 * 提供用户名重名校验和用户注册接口
 * 全部为公开接口，无需登录、无需Token认证
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class RegisterController {

    private final RegisterService registerService;

    public RegisterController(RegisterService registerService) {
        this.registerService = registerService;
    }

    /**
     * 接口1：用户名重名校验
     * POST /api/auth/checkUsername
     * 请求参数：{"username": "学号/工号"}
     * 返回格式：{"code":0,"msg":"success","data":Boolean}
     * data=true 可用，data=false 已存在
     */
    @PostMapping("/checkUsername")
    public DataResponse checkUsername(@RequestBody Map<String, Object> params) {
        DataRequest dataRequest = new DataRequest();
        dataRequest.setData(params);
        return registerService.checkUsername(dataRequest);
    }

    /**
     * 接口2：用户注册
     * POST /api/auth/register
     * 请求参数：{"username":"学号/工号","password":"密码","name":"姓名","role":"角色","userId":"学号/工号","email":"选填","phone":"选填"}
     * 返回格式：
     *   成功：{"code":0,"msg":"success","data":null}
     *   失败：{"code":1,"msg":"错误原因","data":null}
     */
    @PostMapping("/register")
    public DataResponse register(@RequestBody Map<String, Object> params) {
        DataRequest dataRequest = new DataRequest();
        dataRequest.setData(params);
        return registerService.register(dataRequest);
    }
}
