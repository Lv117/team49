package cn.edu.sdu.java.server.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 审批流状态机工具类
 * 定义创新实践项目的状态转换规则
 */
public class ApprovalStateMachine {

    /**
     * 定义有效的状态转换规则
     * key: 当前状态
     * value: 允许转换到的目标状态列表
     */
    private static final Map<String, List<String>> VALID_TRANSITIONS = Map.of(
        "draft", Arrays.asList("submitted"),
        "submitted", Arrays.asList("under_review"),
        "under_review", Arrays.asList("approved", "rejected"),
        "approved", Arrays.asList(),
        "rejected", Arrays.asList("draft")
    );

    /**
     * 检查状态转换是否有效
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     * @return true: 有效转换，false: 无效转换
     */
    public static boolean isValidTransition(String currentStatus, String targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        
        List<String> allowedStatuses = VALID_TRANSITIONS.get(currentStatus);
        return allowedStatuses != null && allowedStatuses.contains(targetStatus);
    }

    /**
     * 获取当前状态允许的所有目标状态
     * @param currentStatus 当前状态
     * @return 允许的目标状态列表
     */
    public static List<String> getAllowedTransitions(String currentStatus) {
        if (currentStatus == null) {
            return Arrays.asList();
        }
        
        List<String> allowedStatuses = VALID_TRANSITIONS.get(currentStatus);
        return allowedStatuses != null ? allowedStatuses : Arrays.asList();
    }

    /**
     * 获取状态转换的错误提示信息
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     * @return 错误提示信息
     */
    public static String getTransitionErrorMessage(String currentStatus, String targetStatus) {
        return "状态转换无效：从 '" + currentStatus + "' 不能转换到 '" + targetStatus + "'";
    }
}
