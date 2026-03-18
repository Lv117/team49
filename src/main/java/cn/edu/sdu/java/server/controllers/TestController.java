package cn.edu.sdu.java.server.controllers;

import cn.edu.sdu.java.server.payload.response.DataResponse;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {

    @GetMapping("/getDemo")
    public DataResponse getDemo() {
        Map<String, Object> demo = new HashMap<>();
        demo.put("message", "Hello from Backend!");
        demo.put("timestamp", System.currentTimeMillis());
        return DataResponse.success(demo);
    }

    @PostMapping("/postDemo")
    public DataResponse postDemo(@RequestBody Map<String, Object> params) {
        System.out.println("接收到的参数：" + params);
        return DataResponse.success("接收成功", params);
    }
}
