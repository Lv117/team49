package cn.edu.sdu.java.server.payload.response;

import lombok.Getter;
import lombok.Setter;

/*
 * DataResponse 前端 HTTP 请求返回数据对象
 * Integer code 返回代码 0 正确返回 1 错误返回信息
 * Object data 返回数据对象
 * String msg 返回正确错误信息
 */
@Setter
@Getter
public class DataResponse {
    private Integer code;
    private Object data;
    private String msg;

    public DataResponse(){

    }

    public DataResponse(Integer code, Object data, String msg) {
        this.code = code;
        this.data = data;
        this.msg = msg;
    }

    public static DataResponse success(Object data) {
        return new DataResponse(0, data, "success");
    }

    public static DataResponse success() {
        return new DataResponse(0, null, "success");
    }

    public static DataResponse success(String msg, Object data) {
        return new DataResponse(0, data, msg);
    }

    public static DataResponse error(String msg) {
        return new DataResponse(1, null, msg);
    }
}
