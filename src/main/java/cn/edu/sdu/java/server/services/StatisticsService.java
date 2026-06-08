package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.RequestLog;
import cn.edu.sdu.java.server.models.StatisticsDay;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.RequestLogRepository;
import cn.edu.sdu.java.server.repositorys.StatisticsDayRepository;
import cn.edu.sdu.java.server.repositorys.UserRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import cn.edu.sdu.java.server.util.DateTimeTool;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class StatisticsService {
    private final UserRepository userRepository;
    private final StatisticsDayRepository statisticsDayRepository;
    private final RequestLogRepository requestLogRepository;
    public StatisticsService(UserRepository userRepository, StatisticsDayRepository statisticsDayRepository,
                             RequestLogRepository requestLogRepository) {
        this.userRepository = userRepository;
        this.statisticsDayRepository = statisticsDayRepository;
        this.requestLogRepository = requestLogRepository;
    }

    public DataResponse getMainPageData(DataRequest dataRequest) {
        Date day = new Date();
        Date monthDay = DateTimeTool.prevMonth(day);
        int i;
        Integer id;
        Object[] a;
        Long l;
        String name;
        long total = userRepository.count();
        Integer monthCount = userRepository.countLastLoginTime(DateTimeTool.parseDateTime(monthDay,"yyyy-MM-dd")+" 00:00:00");
        Integer dayCount = userRepository.countLastLoginTime(DateTimeTool.parseDateTime(day,"yyyy-MM-dd")+" 00:00:00");
        Map<String,Object> data = new HashMap<>();
        Map<String,Object> m = new HashMap<>();
        m.put("total", (int) total);
        m.put("monthCount",monthCount);
        m.put("dayCount",dayCount);
        data.put("onlineUser", m);
        List<?> nList = userRepository.getCountList();
        List<Map<String,Object>> userTypeList = new ArrayList<>();
        for(i= 0;i < nList.size();i++) {
            m = new HashMap<>();
            a = (Object[])nList.get(i);
            id = (Integer)a[0];
            l = (Long)a[1];
            if(id == 1)
                name = "管理员";
            else if(id == 2)
                name = "学生";
            else if(id == 3)
                name = "教师";
            else
                name = "";
            m.put("name", name);
            m.put("value",l.intValue());
            userTypeList.add(m);
        }
        data.put("userTypeList", userTypeList);

        // 从 request_log 计算趋势数据（statistics_day 表可能为空）
        String startRange = DateTimeTool.parseDateTime(monthDay, "yyyy-MM-dd") + " 00:00:00";
        String endRange = DateTimeTool.parseDateTime(day, "yyyy-MM-dd") + " 23:59:59";
        List<RequestLog> requestLogs = requestLogRepository.findByTimeRange(startRange, endRange);

        TreeMap<String, Integer> requestCountByDay = new TreeMap<>();
        TreeMap<String, Set<String>> loginUsersByDay = new TreeMap<>();
        for (RequestLog rl : requestLogs) {
            if (rl.getStartTime() != null && rl.getStartTime().length() >= 10) {
                String dateStr = rl.getStartTime().substring(0, 10);
                String dayKey = dateStr.replace("-", ""); // "yyyyMMdd"
                requestCountByDay.merge(dayKey, 1, Integer::sum);
                if (rl.getUsername() != null && !rl.getUsername().isEmpty()) {
                    loginUsersByDay.computeIfAbsent(dayKey, k -> new HashSet<>()).add(rl.getUsername());
                }
            }
        }

        List<String> dayList = new ArrayList<>(requestCountByDay.keySet());
        List<String> lList = new ArrayList<>();
        List<String> rList = new ArrayList<>();
        List<String> cList = new ArrayList<>();
        List<String> mList2 = new ArrayList<>();
        for (String dayKey : dayList) {
            int reqCount = requestCountByDay.getOrDefault(dayKey, 0);
            int loginCount = loginUsersByDay.containsKey(dayKey) ? loginUsersByDay.get(dayKey).size() : 0;
            rList.add(String.valueOf(reqCount));
            lList.add(String.valueOf(loginCount));
            cList.add(String.valueOf(reqCount));
            mList2.add(String.valueOf(loginCount));
        }
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("value", dayList);
        reqMap.put("label1", lList);
        reqMap.put("label2", rList);
        data.put("requestData", reqMap);
        Map<String, Object> opMap = new HashMap<>();
        opMap.put("value", dayList);
        opMap.put("label1", cList);
        opMap.put("label2", mList2);
        data.put("operateData", opMap);

        return CommonMethod.getReturnData(data);
    }

}
