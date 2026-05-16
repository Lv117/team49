package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;

/**
 * 用户注册服务接口
 * 提供用户名重名校验和用户注册功能
 */
public interface RegisterService {

    /**
     * 校验用户名是否已存在
     * @param dataRequest 请求参数，包含 username
     * @return DataResponse data=true 可用，data=false 已存在
     */
    DataResponse checkUsername(DataRequest dataRequest);

    /**
     * 用户注册
     * @param dataRequest 请求参数，包含 username, password, name, role, userId, email(选填), phone(选填)
     * @return DataResponse code=0 成功，code=1 失败
     */
    DataResponse register(DataRequest dataRequest);
}