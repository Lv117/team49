package cn.edu.sdu.java.server.services;

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
    
    public ConsumptionService(FeeRepository feeRepository, StudentRepository studentRepository) {
        this.feeRepository = feeRepository;
        this.studentRepository = studentRepository;
    }
    
    /**
     * 获取消费记录列表
     */
    public DataResponse getConsumptionList(DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");
        String consumptionType = dataRequest.getString("consumptionType");
        String keyword = dataRequest.getString("keyword");
        
        if (personId == null) {
            personId = 0;
        }
        
        List<Fee> feeList = feeRepository.findByConditions(personId, consumptionType, keyword);
        
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
        Map<String, Object> form = dataRequest.getData();
        if (form == null || form.isEmpty()) {
            form = dataRequest.getMap("form");
        }
        
        Integer feeId = CommonMethod.getInteger(form, "feeId");
        Integer personId = CommonMethod.getInteger(form, "personId");
        String consumptionType = CommonMethod.getString(form, "consumptionType");
        Double money = CommonMethod.getDouble(form, "money");
        String day = CommonMethod.getString(form, "day");
        String description = CommonMethod.getString(form, "description");
        
        if (personId == null || personId <= 0) {
            return CommonMethod.getReturnMessageError("学生ID不能为空");
        }
        if (consumptionType == null || consumptionType.isEmpty()) {
            return CommonMethod.getReturnMessageError("消费类型不能为空");
        }
        if (money == null || money <= 0) {
            return CommonMethod.getReturnMessageError("消费金额必须大于0");
        }
        if (day == null || day.isEmpty()) {
            return CommonMethod.getReturnMessageError("消费日期不能为空");
        }
        
        Fee fee;
        
        // 更新或新建
        if (feeId != null && feeId > 0) {
            Optional<Fee> op = feeRepository.findById(feeId);
            if (op.isPresent()) {
                fee = op.get();
            } else {
                return CommonMethod.getReturnMessageError("消费记录不存在");
            }
        } else {
            fee = new Fee();
            Optional<Student> studentOp = studentRepository.findById(personId);
            if (studentOp.isPresent()) {
                fee.setStudent(studentOp.get());
            } else {
                return CommonMethod.getReturnMessageError("学生不存在");
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
        
        if (feeId != null && feeId > 0) {
            Optional<Fee> op = feeRepository.findById(feeId);
            op.ifPresent(feeRepository::delete);
        }
        
        return CommonMethod.getReturnMessageOK();
    }
    
    /**
     * 获取月度消费统计
     */
    public DataResponse getMonthlyConsumptionStats(DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");
        String yearMonth = dataRequest.getString("yearMonth");
        
        if (personId == null || personId <= 0) {
            return CommonMethod.getReturnMessageError("学生ID不能为空");
        }
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
        String uploader = CommonMethod.getString(dataRequest.getData(), "uploader");
        
        if (personId == null || personId <= 0) {
            return CommonMethod.getReturnMessageError("学生ID不能为空");
        }
        if (file == null || file.isEmpty()) {
            return CommonMethod.getReturnMessageError("文件不能为空");
        }
        
        try (InputStream inputStream = file.getInputStream()) {
            return importFromExcel(inputStream, personId);
        } catch (IOException e) {
            log.error("导入消费数据失败", e);
            return CommonMethod.getReturnMessageError("导入失败: " + e.getMessage());
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
                return CommonMethod.getReturnMessageError("学生不存在");
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
            return CommonMethod.getReturnMessageError("读取Excel文件失败: " + e.getMessage());
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
        map.put("feeId", fee.getFeeId());
        map.put("personId", fee.getStudent() != null ? fee.getStudent().getPersonId() : null);
        map.put("studentName", fee.getStudent() != null && fee.getStudent().getPerson() != null ? fee.getStudent().getPerson().getName() : "");
        map.put("day", fee.getDay());
        map.put("money", fee.getMoney());
        map.put("consumptionType", fee.getConsumptionType());
        map.put("consumptionTypeName", getConsumptionTypeName(fee.getConsumptionType()));
        map.put("description", fee.getDescription());
        map.put("createTime", fee.getCreateTime());
        return map;
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
}
