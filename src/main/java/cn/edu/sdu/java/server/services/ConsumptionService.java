package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.exception.BusinessException;
import cn.edu.sdu.java.server.exception.ErrorCodes;
import cn.edu.sdu.java.server.models.Fee;
import cn.edu.sdu.java.server.models.Student;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.FeeRepository;
import cn.edu.sdu.java.server.repositorys.StudentRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Consumption 消费日志服务类
 */
@Service
public class ConsumptionService {
    private static final Logger log = LoggerFactory.getLogger(ConsumptionService.class);
    
    private final FeeRepository feeRepository;
    private final StudentRepository studentRepository;
    private final TeacherDataScopeService teacherDataScopeService;
    
    public ConsumptionService(FeeRepository feeRepository,
                              StudentRepository studentRepository,
                              TeacherDataScopeService teacherDataScopeService) {
        this.feeRepository = feeRepository;
        this.studentRepository = studentRepository;
        this.teacherDataScopeService = teacherDataScopeService;
    }
    
    /**
     * 获取消费记录列表
     */
    public DataResponse getConsumptionList(DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");
        if (personId == null || personId <= 0) {
            personId = dataRequest.getInteger("studentId");
        }
        personId = resolveStudentIdForQuery(personId, "教师仅可查看本人授课学生的消费记录");
        String consumptionType = dataRequest.getString("consumptionType");
        String keyword = dataRequest.getString("keyword");
        String month = dataRequest.getString("month");
        
        if (personId == null) {
            personId = 0;
        }
        
        // 如果传了 month 参数（如 "2026-05"），用月份前缀过滤
        if (month != null && !month.isEmpty()) {
            keyword = month;
        }
        
        List<Fee> feeList = feeRepository.findByConditions(personId, consumptionType, keyword);
        feeList = filterFeesForTeacher(feeList);
        
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (Fee fee : feeList) {
            dataList.add(getMapFromFee(fee));
        }
        
        return CommonMethod.getReturnData(dataList);
    }
    
    /**
     * 保存消费记录
     */
    public DataResponse consumptionSave(DataRequest dataRequest) {
        // 前端可能把参数放在 data 字段里，需要兼容两种情况
        Map<String, Object> form = dataRequest.getMap("data");
        if (form == null || form.isEmpty()) {
            form = dataRequest.getData();
        }
        if (form == null || form.isEmpty()) {
            form = dataRequest.getMap("form");
        }
        
        // 支持前端字段映射: consumptionId -> feeId, consumptionDate -> day, amount -> money, remark -> description
        Integer feeId = CommonMethod.getInteger(form, "feeId");
        if (feeId == null || feeId <= 0) {
            feeId = CommonMethod.getInteger(form, "consumptionId");
        }
        
        // 支持 studentId 和 personId 两种字段名
        Integer personId = CommonMethod.getInteger(form, "personId");
        if (personId == null || personId <= 0) {
            personId = CommonMethod.getInteger(form, "studentId");
        }
        personId = resolveRequiredStudentId(personId, "学生ID不能为空", "教师仅可维护本人授课学生的消费记录");
        String consumptionType = CommonMethod.getString(form, "consumptionType");
        Double money = CommonMethod.getDouble(form, "money");
        if (money == null || money <= 0) {
            money = CommonMethod.getDouble(form, "amount");
        }
        
        String day = CommonMethod.getString(form, "day");
        if (day == null || day.isEmpty()) {
            day = CommonMethod.getString(form, "consumptionDate");
        }
        
        String description = CommonMethod.getString(form, "description");
        if (description == null || description.isEmpty()) {
            description = CommonMethod.getString(form, "remark");
        }
        
        if (consumptionType == null || consumptionType.isEmpty()) {
            throw new BusinessException(ErrorCodes.CONSUMPTION_TYPE_REQUIRED, "消费类型不能为空");
        }
        if (money == null || money <= 0) {
            throw new BusinessException(ErrorCodes.CONSUMPTION_AMOUNT_INVALID, "消费金额必须大于0");
        }
        if (day == null || day.isEmpty()) {
            throw new BusinessException(ErrorCodes.CONSUMPTION_DATE_REQUIRED, "消费日期不能为空");
        }
        
        Fee fee;
        
        // 更新或新建
        if (feeId != null && feeId > 0) {
            Optional<Fee> op = feeRepository.findById(feeId);
            if (op.isPresent()) {
                fee = op.get();
                assertStudentAccessible(getStudentId(fee), "教师仅可维护本人授课学生的消费记录");
            } else {
                throw new BusinessException(ErrorCodes.CONSUMPTION_NOT_FOUND, "消费记录不存在");
            }
        } else {
            fee = new Fee();
            Optional<Student> studentOp = studentRepository.findById(personId);
            if (studentOp.isPresent()) {
                fee.setStudent(studentOp.get());
            } else {
                throw new BusinessException(ErrorCodes.CONSUMPTION_STUDENT_NOT_FOUND, "学生不存在");
            }
        }
        
        fee.setConsumptionType(consumptionType);
        fee.setMoney(money);
        fee.setDay(day);
        fee.setDescription(description);
        
        feeRepository.save(fee);
        
        return CommonMethod.getReturnMessageOK();
    }
    
    /**
     * 删除消费记录
     */
    public DataResponse consumptionDelete(DataRequest dataRequest) {
        Integer feeId = dataRequest.getInteger("feeId");
        if (feeId == null || feeId <= 0) {
            throw new BusinessException(ErrorCodes.CONSUMPTION_ID_REQUIRED, "消费记录ID不能为空");
        }
        Fee fee = feeRepository.findById(feeId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.CONSUMPTION_NOT_FOUND, "消费记录不存在"));
        assertStudentAccessible(getStudentId(fee), "教师仅可删除本人授课学生的消费记录");
        feeRepository.delete(fee);
        return CommonMethod.getReturnMessageOK();
    }
    
    /**
     * 获取月度消费统计
     */
    public DataResponse getMonthlyConsumptionStats(DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");
        personId = resolveRequiredStudentId(personId, "学生ID不能为空", "教师仅可查看本人授课学生的月度消费统计");
        String yearMonth = dataRequest.getString("yearMonth");
        
        if (yearMonth == null || yearMonth.isEmpty()) {
            // 默认当前月份
            yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        
        // 解析年月
        String startDate = yearMonth + "-01";
        String endDate = yearMonth + "-31";
        
        // 统计各类型消费
        Map<String, Double> stats = new LinkedHashMap<>();
        String[] types = {"dining", "study", "transport", "life", "entertainment"};
        String[] typeNames = {"餐饮消费", "学习用品", "交通费", "生活用品", "娱乐消费"};
        
        for (int i = 0; i < types.length; i++) {
            List<Fee> feeList = feeRepository.findByStudentAndTypeAndDateRange(personId, types[i], startDate, endDate);
            double total = feeList.stream().mapToDouble(Fee::getMoney).sum();
            stats.put(typeNames[i], total);
        }
        
        // 计算总消费
        double totalConsumption = stats.values().stream().mapToDouble(Double::doubleValue).sum();
        stats.put("total", totalConsumption);
        
        // 转换为前端需要的格式
        List<Map<String, Object>> chartData = new ArrayList<>();
        for (Map.Entry<String, Double> entry : stats.entrySet()) {
            if (!"total".equals(entry.getKey())) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", entry.getKey());
                item.put("value", entry.getValue());
                chartData.add(item);
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("stats", stats);
        result.put("chartData", chartData);
        result.put("yearMonth", yearMonth);
        
        return CommonMethod.getReturnData(result);
    }
    
    /**
     * Excel批量导入消费数据
     */
    public DataResponse importConsumptionData(DataRequest dataRequest, MultipartFile file) {
        Integer personId = CommonMethod.getInteger(dataRequest.getData(), "personId");
        personId = resolveRequiredStudentId(personId, "学生ID不能为空", "教师仅可导入本人授课学生的消费数据");
        
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCodes.CONSUMPTION_FILE_EMPTY, "文件不能为空");
        }
        
        try (InputStream inputStream = file.getInputStream()) {
            return importFromExcel(inputStream, personId);
        } catch (IOException e) {
            log.error("导入消费数据失败", e);
            throw new BusinessException(ErrorCodes.CONSUMPTION_IMPORT_FAILED, "导入失败，请检查文件内容或稍后重试");
        }
    }
    
    /**
     * 从Excel导入消费数据
     */
    private DataResponse importFromExcel(InputStream inputStream, Integer personId) {
        try {
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0);
            
            Optional<Student> studentOp = studentRepository.findById(personId);
            if (studentOp.isEmpty()) {
                throw new BusinessException(ErrorCodes.CONSUMPTION_STUDENT_NOT_FOUND, "学生不存在");
            }
            Student student = studentOp.get();
            
            int successCount = 0;
            int failCount = 0;
            
            // 从第二行开始读取(第一行是标题)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                
                try {
                    Fee fee = new Fee();
                    fee.setStudent(student);
                    
                    // 读取日期(第1列)
                    Cell dayCell = row.getCell(0);
                    if (dayCell != null) {
                        fee.setDay(getCellValueAsString(dayCell));
                    }
                    
                    // 读取消费类型(第2列)
                    Cell typeCell = row.getCell(1);
                    if (typeCell != null) {
                        String typeName = getCellValueAsString(typeCell);
                        fee.setConsumptionType(parseConsumptionType(typeName));
                    }
                    
                    // 读取金额(第3列)
                    Cell moneyCell = row.getCell(2);
                    if (moneyCell != null) {
                        String moneyStr = getCellValueAsString(moneyCell);
                        fee.setMoney(Double.parseDouble(moneyStr));
                    }
                    
                    // 读取备注(第4列)
                    Cell descCell = row.getCell(3);
                    if (descCell != null) {
                        fee.setDescription(getCellValueAsString(descCell));
                    }
                    
                    feeRepository.save(fee);
                    successCount++;
                } catch (Exception e) {
                    log.warn("导入第{}行数据失败: {}", i + 1, e.getMessage());
                    failCount++;
                }
            }
            
            workbook.close();
            
            Map<String, Object> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            
            return CommonMethod.getReturnData(result);
        } catch (IOException e) {
            log.error("读取Excel文件失败", e);
            throw new BusinessException(ErrorCodes.CONSUMPTION_IMPORT_FAILED, "读取Excel文件失败，请检查文件格式");
        }
    }
    
    /**
     * 获取单元格值为字符串
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                } else {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }
    
    /**
     * 解析消费类型
     */
    private String parseConsumptionType(String typeName) {
        if (typeName == null) {
            return "other";
        }
        return switch (typeName.trim()) {
            case "餐饮", "餐饮消费", "dining" -> "dining";
            case "学习用品", "study" -> "study";
            case "交通", "交通费", "transport" -> "transport";
            case "生活用品", "life" -> "life";
            case "娱乐", "娱乐消费", "entertainment" -> "entertainment";
            default -> "other";
        };
    }
    
    /**
     * 将 Fee 转换为 Map
     */
    private Map<String, Object> getMapFromFee(Fee fee) {
        Map<String, Object> map = new HashMap<>();
        // 同时返回后端字段名和前端期望的字段名，确保兼容性
        map.put("feeId", fee.getFeeId());
        map.put("consumptionId", fee.getFeeId()); // 前端期望的字段名
        map.put("personId", fee.getStudent() != null ? fee.getStudent().getPersonId() : null);
        map.put("studentName", fee.getStudent() != null && fee.getStudent().getPerson() != null ? fee.getStudent().getPerson().getName() : "");
        map.put("day", fee.getDay());
        map.put("consumptionDate", fee.getDay()); // 前端期望的字段名
        map.put("money", fee.getMoney());
        map.put("amount", fee.getMoney()); // 前端期望的字段名
        map.put("consumptionType", fee.getConsumptionType());
        map.put("consumptionTypeName", getConsumptionTypeName(fee.getConsumptionType()));
        map.put("description", fee.getDescription());
        map.put("remark", fee.getDescription()); // 前端期望的字段名
        map.put("createTime", fee.getCreateTime());
        return map;
    }
    
    /**
     * 获取消费账单列表（按月份分类统计）
     */
    public DataResponse getConsumptionBillList(DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");
        personId = resolveRequiredStudentId(personId, "学生ID不能为空", "教师仅可查看本人授课学生的消费账单");
        String yearMonth = dataRequest.getString("yearMonth");
        
        if (yearMonth == null || yearMonth.isEmpty()) {
            yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        
        // 获取该月份的所有消费记录
        String startDate = yearMonth + "-01";
        String endDate = yearMonth + "-31";
        
        List<Fee> feeList = feeRepository.findByStudentAndDateRange(personId, startDate, endDate);
        
        // 按消费类型分类统计
        Map<String, Object> billData = new LinkedHashMap<>();
        Map<String, Double> typeStats = new LinkedHashMap<>();
        Map<String, List<Map<String, Object>>> typeDetails = new LinkedHashMap<>();
        
        String[] types = {"dining", "study", "transport", "life", "entertainment"};
        String[] typeNames = {"餐饮消费", "学习用品", "交通费", "生活用品", "娱乐消费"};
        
        double totalAmount = 0;
        
        for (int i = 0; i < types.length; i++) {
            final int index = i;
            String type = types[index];
            String typeName = typeNames[index];
            
            List<Fee> typeFees = feeList.stream()
                    .filter(f -> type.equals(f.getConsumptionType()))
                    .toList();
            
            double typeTotal = typeFees.stream().mapToDouble(Fee::getMoney).sum();
            typeStats.put(typeName, typeTotal);
            totalAmount += typeTotal;
            
            // 收集该类型的详细记录
            List<Map<String, Object>> details = new ArrayList<>();
            for (Fee fee : typeFees) {
                details.add(getMapFromFee(fee));
            }
            typeDetails.put(typeName, details);
        }
        
        billData.put("yearMonth", yearMonth);
        billData.put("totalAmount", totalAmount);
        billData.put("typeStats", typeStats);
        billData.put("typeDetails", typeDetails);
        
        return CommonMethod.getReturnData(billData);
    }
    
    /**
     * 检查异常消费（预警）
     */
    public DataResponse checkAbnormalConsumption(DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");
        personId = resolveRequiredStudentId(personId, "学生ID不能为空", "教师仅可查看本人授课学生的消费预警");
        Double singleThreshold = dataRequest.getDouble("singleThreshold"); // 单笔超过阈值
        Double dailyThreshold = dataRequest.getDouble("dailyThreshold");   // 单日消费过高
        Double frequencyThreshold = dataRequest.getDouble("frequencyThreshold"); // 消费频率异常
        
        // 默认阈值
        if (singleThreshold == null || singleThreshold <= 0) {
            singleThreshold = 500.0; // 单笔超过500元
        }
        if (dailyThreshold == null || dailyThreshold <= 0) {
            dailyThreshold = 1000.0; // 单日超过1000元
        }
        if (frequencyThreshold == null || frequencyThreshold <= 0) {
            frequencyThreshold = 10.0; // 单日消费次数超过10次
        }
        
        List<Fee> allFees = feeRepository.findByStudentId(personId);
        List<Map<String, Object>> abnormalList = new ArrayList<>();
        
        // 检查单笔超过阈值
        for (Fee fee : allFees) {
            if (fee.getMoney() > singleThreshold) {
                Map<String, Object> abnormal = new HashMap<>();
                abnormal.put("type", "单笔超额");
                abnormal.put("fee", getMapFromFee(fee));
                abnormal.put("threshold", singleThreshold);
                abnormal.put("amount", fee.getMoney());
                abnormal.put("excess", fee.getMoney() - singleThreshold);
                abnormalList.add(abnormal);
            }
        }
        
        // 检查单日消费过高
        Map<String, Double> dailyStats = new HashMap<>();
        Map<String, List<Fee>> dailyFees = new HashMap<>();
        
        for (Fee fee : allFees) {
            String day = fee.getDay();
            dailyStats.put(day, dailyStats.getOrDefault(day, 0.0) + fee.getMoney());
            dailyFees.computeIfAbsent(day, k -> new ArrayList<>()).add(fee);
        }
        
        for (Map.Entry<String, Double> entry : dailyStats.entrySet()) {
            if (entry.getValue() > dailyThreshold) {
                Map<String, Object> abnormal = new HashMap<>();
                abnormal.put("type", "单日超额");
                abnormal.put("day", entry.getKey());
                abnormal.put("threshold", dailyThreshold);
                abnormal.put("totalAmount", entry.getValue());
                abnormal.put("excess", entry.getValue() - dailyThreshold);
                abnormal.put("count", dailyFees.get(entry.getKey()).size());
                abnormal.put("details", dailyFees.get(entry.getKey()).stream()
                        .map(this::getMapFromFee).toList());
                abnormalList.add(abnormal);
            }
        }
        
        // 检查消费频率异常
        Map<String, Integer> frequencyStats = new HashMap<>();
        for (Fee fee : allFees) {
            String day = fee.getDay();
            frequencyStats.put(day, frequencyStats.getOrDefault(day, 0) + 1);
        }
        
        for (Map.Entry<String, Integer> entry : frequencyStats.entrySet()) {
            if (entry.getValue() > frequencyThreshold) {
                Map<String, Object> abnormal = new HashMap<>();
                abnormal.put("type", "频率异常");
                abnormal.put("day", entry.getKey());
                abnormal.put("threshold", frequencyThreshold);
                abnormal.put("frequency", entry.getValue());
                abnormal.put("details", dailyFees.get(entry.getKey()).stream()
                        .map(this::getMapFromFee).toList());
                abnormalList.add(abnormal);
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("abnormalCount", abnormalList.size());
        result.put("abnormalList", abnormalList);
        result.put("thresholds", Map.of(
                "singleThreshold", singleThreshold,
                "dailyThreshold", dailyThreshold,
                "frequencyThreshold", frequencyThreshold
        ));
        
        return CommonMethod.getReturnData(result);
    }
    
    /**
     * 获取消费类型名称
     */
    private String getConsumptionTypeName(String consumptionType) {
        if (consumptionType == null) {
            return "其他";
        }
        return switch (consumptionType) {
            case "dining" -> "餐饮消费";
            case "study" -> "学习用品";
            case "transport" -> "交通费";
            case "life" -> "生活用品";
            case "entertainment" -> "娱乐消费";
            default -> "其他";
        };
    }

    private Integer resolveStudentIdForQuery(Integer studentId, String teacherMessage) {
        if ("ROLE_STUDENT".equals(CommonMethod.getRoleName())) {
            Integer currentPersonId = CommonMethod.getPersonId();
            if (currentPersonId == null || currentPersonId <= 0) {
                throw new BusinessException(ErrorCodes.AUTH_FAILED, "当前登录状态无效，请重新登录");
            }
            return currentPersonId;
        }
        if (studentId != null && studentId > 0) {
            teacherDataScopeService.assertCurrentTeacherAccessStudent(studentId, teacherMessage);
        }
        return studentId;
    }

    private Integer resolveRequiredStudentId(Integer studentId, String emptyMessage, String teacherMessage) {
        Integer resolvedStudentId = resolveStudentIdForQuery(studentId, teacherMessage);
        if (resolvedStudentId == null || resolvedStudentId <= 0) {
            throw new BusinessException(ErrorCodes.CONSUMPTION_STUDENT_REQUIRED, emptyMessage);
        }
        return resolvedStudentId;
    }

    private void assertStudentAccessible(Integer studentId, String teacherMessage) {
        if ("ROLE_STUDENT".equals(CommonMethod.getRoleName())) {
            Integer currentPersonId = CommonMethod.getPersonId();
            if (currentPersonId == null || currentPersonId <= 0) {
                throw new BusinessException(ErrorCodes.AUTH_FAILED, "当前登录状态无效，请重新登录");
            }
            if (!currentPersonId.equals(studentId)) {
                throw new BusinessException(ErrorCodes.ACCESS_DENIED, "学生只能操作自己的消费数据");
            }
        }
        teacherDataScopeService.assertCurrentTeacherAccessStudent(studentId, teacherMessage);
    }

    private Integer getStudentId(Fee fee) {
        return fee == null || fee.getStudent() == null ? null : fee.getStudent().getPersonId();
    }

    private List<Fee> filterFeesForTeacher(List<Fee> feeList) {
        if (!teacherDataScopeService.isCurrentRoleTeacher()) {
            return feeList;
        }
        Set<Integer> studentIds = teacherDataScopeService.getCurrentTeacherStudentIds();
        return feeList.stream()
                .filter(fee -> studentIds.contains(getStudentId(fee)))
                .toList();
    }
}
