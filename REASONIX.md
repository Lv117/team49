# Reasonix project memory

Notes the user pinned via the `#` prompt prefix. The whole file is
loaded into the immutable system prefix every session — keep it terse.

- 角色与任务背景
你是一个资深的 JavaFX 前端开发专家。当前我们需要对教务系统中的“学生请假管理”模块进行前端完善。
现有的核心控制器文件为 `StudentLeaveController.java`，对应的 UI 文件为 `student-leave-panel.fxml`。
请你仔细阅读现有的代码上下文，并严格按照以下 4 个步骤完成前端代码的重构与新增功能开发。

# 核心需求目标
1. **审批进度列优化**：在 TableView 中完善“审批进度”列，使用进度条+状态文案展示（待审核、教师审批、管理员审批、已完成、已驳回），支持点击查看详情。
2. **审批流程可视化弹窗**：在详情弹窗中绘制垂直时间线，展示每个节点的操作人、时间、意见。
3. **考勤同步标记**：审批通过后（已完成状态），在列表和弹窗中显著展示“已同步到考勤系统”标记。

---

# 分步执行指南

## 步骤一：修改 FXML 文件 (`student-leave-panel.fxml`)
1. 找到 `<TableView fx:id="dataTableView">` 下的 `<columns>`。
2. 确保存在 `<TableColumn fx:id="progressColumn" text="审批进度" prefWidth="240"/>`。如果宽度不够请调整为 240 以容纳进度条和同步标记。
3. 检查并确保其他列（学号、姓名、教师、日期、事由、状态、教师意见、管理员意见）的 `fx:id` 与 Controller 中的 `@FXML` 属性严格对应。

## 步骤二：优化 Controller 中的表格列与进度条逻辑
在 `StudentLeaveController.java` 中，重构 `progressColumn.setCellFactory(...)` 的逻辑：
1. **状态映射规范化**：
   - `state == 0`: 进度 0.25，橙色，文案 "待审核"
   - `state == 1`: 进度 0.50，蓝色，文案 "教师审批中"
   - `state == 2`: 进度 0.75，橙色，文案 "管理员审批"
   - `state == 3`: 进度 1.00，绿色，文案 "已完成"
   - `state == 4`: 进度 1.00，红色，文案 "已驳回"
2. **增加考勤同步标记**：
   - 在单元格的 `HBox` 中新增一个 `Label syncLabel`，文案为 "✅已同步考勤"，字体颜色为深绿色 `#2E7D32`，默认隐藏。
   - 逻辑判断：读取当前行数据的 `synced` 字段（布尔值），当 `state == 3` 且 `synced == true` 时，设置 `syncLabel.setVisible(true)`。
3. **点击事件**：保留点击单元格触发 `showProgressTimeline(id)` 的逻辑。

## 步骤三：重构审批时间线可视化弹窗 (`showProgressTimeline` 方法)
完全重写或优化 `showProgressTimeline` 方法，使其 UI 更加美观专业：
1. **数据解析**：从后端接口获取 `progressTimeline` (List<Map>) 和 `synced` (Boolean)。
2. **时间线 UI 绘制**：
   - 使用 `VBox` 作为主容器。
   - 遍历 `timeline` 列表，为每个节点创建一个 `HBox`（左侧指示器 + 右侧内容）。
   - **左侧指示器**：使用 `Circle` (圆点) + `Region` (竖线)。根据节点状态（completed/rejected/processing/pending）动态改变圆点和竖线的颜色（绿/红/蓝/灰）。
   - **右侧内容**：使用 `VBox` 包含：
     - 标题行：状态 Emoji + 阶段名称 (如 "✅ 教师审批")，加粗。
     - 详情行：🕒 时间 + 👤 操作人，灰色小字。
     - 意见行：💬 审批意见，斜体灰色小字（若为空则不显示）。
3. **底部考勤同步提示**：
   - 在时间线最下方添加一条 `Separator`。
   - 如果 `synced == true`，在底部添加一个 `HBox`，显示 "🔄 请假数据已自动同步至考勤管理系统"，使用醒目的绿色加粗字体。
4. **弹窗配置**：使用 `Dialog` 或自定义 `Stage`，设置合适的宽高（如 500x450），包裹在 `ScrollPane` 中以防内容过长。

## 步骤四：对齐后端数据结构（前端解析契约）
在编写解析代码时，请确保前端能够健壮地处理以下 JSON 数据结构（使用 `CommonMethod.getString` / `getInteger` 等工具类防空指针）：

**列表接口返回的单条数据结构：**
```json
{
  "studentLeaveId": 1,
  "state": 3,
  "synced": true,  // 核心：是否已同步考勤
  "studentNum": "2021001",
  "studentName": "张三",
  "stateName": "已完成"
}
时间线详情接口 (/api/studentLeave/getLeaveProgress) 返回结构：
json

编辑



{
  "currentStatus": "已完成",
  "synced": true,
  "progressTimeline": [
    {
      "step": 1,
      "stage": "学生提交申请",
      "status": "completed",
      "operator": "张三",
      "timestamp": "2026-05-28T10:00:00",
      "comment": ""
    },
    {
      "step": 2,
      "stage": "教师审批",
      "status": "completed",
      "operator": "李老师",
      "timestamp": "2026-05-28T11:30:00",
      "comment": "同意请假，注意安全"
    }
  ]
}
代码规范与约束
JavaFX 最佳实践：所有 UI 组件的创建和更新必须在 JavaFX 应用程序线程（FX Thread）中进行。
样式分离：尽量使用内联 CSS (setStyle) 快速实现样式，保持代码紧凑；颜色使用 Material Design 规范色值（如绿 #4CAF50，红 #F44336，蓝 #2196F3）。
空值安全：在解析 Map 数据时，必须处理 null 值，避免 NullPointerException。
角色权限控制：保留并优化原有的 initialize 方法中的角色判断逻辑（学生、教师、管理员），确保按钮和输入框的禁用/隐藏状态正确。
清理冗余：移除原有代码中重复的 setCellValueFactory 设置（原代码中 studentNameColumn 设置了两次），精简 import 列表。
请现在开始执行，先输出修改后的 student-leave-panel.fxml 核心 columns 部分，然后输出完整的 StudentLeaveController.java 代码。
