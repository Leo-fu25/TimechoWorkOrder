package com.timecho.workorder.config;

import com.timecho.workorder.model.Department;
import com.timecho.workorder.model.AssignmentRule;
import com.timecho.workorder.model.FeishuWebhookConfig;
import com.timecho.workorder.model.Priority;
import com.timecho.workorder.model.SlaPolicy;
import com.timecho.workorder.model.Status;
import com.timecho.workorder.model.User;
import com.timecho.workorder.model.WorkOrderType;
import com.timecho.workorder.service.AssignmentRuleService;
import com.timecho.workorder.service.DepartmentService;
import com.timecho.workorder.service.FeishuWebhookConfigService;
import com.timecho.workorder.service.PriorityService;
import com.timecho.workorder.service.SlaPolicyService;
import com.timecho.workorder.service.StatusService;
import com.timecho.workorder.service.UserService;
import com.timecho.workorder.service.WorkOrderTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {
    @Autowired
    private DepartmentService departmentService;
    
    @Autowired
    private StatusService statusService;
    
    @Autowired
    private PriorityService priorityService;
    
    @Autowired
    private UserService userService;

    @Autowired
    private WorkOrderTypeService workOrderTypeService;

    @Autowired
    private AssignmentRuleService assignmentRuleService;

    @Autowired
    private SlaPolicyService slaPolicyService;

    @Autowired
    private FeishuWebhookConfigService feishuWebhookConfigService;
    
    @Override
    public void run(String... args) throws Exception {
        // 初始化部门
        if (departmentService.getAllDepartments().isEmpty()) {
            Department itDepartment = new Department();
            itDepartment.setName("IT部门");
            itDepartment.setDescription("负责信息技术相关工作");
            itDepartment.setActive(true);
            departmentService.createDepartment(itDepartment);
            
            Department hrDepartment = new Department();
            hrDepartment.setName("人力资源部");
            hrDepartment.setDescription("负责人力资源管理工作");
            hrDepartment.setActive(true);
            departmentService.createDepartment(hrDepartment);
            
            Department financeDepartment = new Department();
            financeDepartment.setName("财务部");
            financeDepartment.setDescription("负责财务管理工作");
            financeDepartment.setActive(true);
            departmentService.createDepartment(financeDepartment);
        }
        
        // 初始化状态
        if (statusService.getAllStatuses().isEmpty()) {
            Status pendingStatus = new Status();
            pendingStatus.setName("PENDING");
            pendingStatus.setDescription("待处理");
            pendingStatus.setActive(true);
            statusService.createStatus(pendingStatus);
            
            Status inProgressStatus = new Status();
            inProgressStatus.setName("IN_PROGRESS");
            inProgressStatus.setDescription("处理中");
            inProgressStatus.setActive(true);
            statusService.createStatus(inProgressStatus);
            
            Status completedStatus = new Status();
            completedStatus.setName("COMPLETED");
            completedStatus.setDescription("已完成");
            completedStatus.setActive(true);
            statusService.createStatus(completedStatus);
            
            Status cancelledStatus = new Status();
            cancelledStatus.setName("CANCELLED");
            cancelledStatus.setDescription("已取消");
            cancelledStatus.setActive(true);
            statusService.createStatus(cancelledStatus);
        }
        
        // 初始化优先级
        if (priorityService.getAllPriorities().isEmpty()) {
            Priority lowPriority = new Priority();
            lowPriority.setName("LOW");
            lowPriority.setDescription("低优先级");
            lowPriority.setLevel(1);
            lowPriority.setActive(true);
            priorityService.createPriority(lowPriority);
            
            Priority mediumPriority = new Priority();
            mediumPriority.setName("MEDIUM");
            mediumPriority.setDescription("中优先级");
            mediumPriority.setLevel(2);
            mediumPriority.setActive(true);
            priorityService.createPriority(mediumPriority);
            
            Priority highPriority = new Priority();
            highPriority.setName("HIGH");
            highPriority.setDescription("高优先级");
            highPriority.setLevel(3);
            highPriority.setActive(true);
            priorityService.createPriority(highPriority);
            
            Priority urgentPriority = new Priority();
            urgentPriority.setName("URGENT");
            urgentPriority.setDescription("紧急");
            urgentPriority.setLevel(4);
            urgentPriority.setActive(true);
            priorityService.createPriority(urgentPriority);
        }

        // 初始化工单类型
        if (workOrderTypeService.getAllTypes().isEmpty()) {
            WorkOrderType demandType = new WorkOrderType();
            demandType.setName("DEMAND");
            demandType.setDescription("需求");
            demandType.setActive(true);
            workOrderTypeService.createType(demandType);

            WorkOrderType bugType = new WorkOrderType();
            bugType.setName("BUG");
            bugType.setDescription("缺陷");
            bugType.setActive(true);
            workOrderTypeService.createType(bugType);

            WorkOrderType consultationType = new WorkOrderType();
            consultationType.setName("CONSULTATION");
            consultationType.setDescription("咨询");
            consultationType.setActive(true);
            workOrderTypeService.createType(consultationType);

            WorkOrderType incidentType = new WorkOrderType();
            incidentType.setName("INCIDENT");
            incidentType.setDescription("故障");
            incidentType.setActive(true);
            workOrderTypeService.createType(incidentType);
        }
        
        // 初始化用户
        if (userService.getAllUsers().isEmpty()) {
            List<Department> departments = departmentService.getAllDepartments();
            if (!departments.isEmpty()) {
                Department itDepartment = departments.get(0);
                
                User adminUser = new User();
                adminUser.setUsername("admin");
                adminUser.setPassword("admin123");
                adminUser.setEmail("admin@example.com");
                adminUser.setName("管理员");
                adminUser.setDepartment(itDepartment);
                adminUser.setActive(true);
                userService.createUser(adminUser);
                
                User normalUser = new User();
                normalUser.setUsername("user");
                normalUser.setPassword("user123");
                normalUser.setEmail("user@example.com");
                normalUser.setName("普通用户");
                normalUser.setDepartment(itDepartment);
                normalUser.setActive(true);
                userService.createUser(normalUser);

                User portalBot = new User();
                portalBot.setUsername("portal_bot");
                portalBot.setPassword("portal_bot_123");
                portalBot.setEmail("portal-bot@example.com");
                portalBot.setName("客户门户机器人");
                portalBot.setDepartment(itDepartment);
                portalBot.setActive(true);
                userService.createUser(portalBot);
            }
        }

        // 初始化 SLA 策略
        if (slaPolicyService.getAllPolicies().isEmpty()) {
            List<Priority> priorities = priorityService.getAllPriorities();
            Priority low = priorities.stream().filter(priority -> "LOW".equalsIgnoreCase(priority.getName())).findFirst().orElse(null);
            Priority medium = priorities.stream().filter(priority -> "MEDIUM".equalsIgnoreCase(priority.getName())).findFirst().orElse(null);
            Priority high = priorities.stream().filter(priority -> "HIGH".equalsIgnoreCase(priority.getName())).findFirst().orElse(null);
            Priority urgent = priorities.stream().filter(priority -> "URGENT".equalsIgnoreCase(priority.getName())).findFirst().orElse(null);

            SlaPolicy defaultPolicy = new SlaPolicy();
            defaultPolicy.setName("默认SLA");
            defaultPolicy.setResponseHours(8);
            defaultPolicy.setResolveHours(48);
            defaultPolicy.setAutoEscalate(true);
            defaultPolicy.setEscalationPriority(urgent);
            defaultPolicy.setActive(true);
            slaPolicyService.createPolicy(defaultPolicy);

            if (high != null) {
                SlaPolicy highPolicy = new SlaPolicy();
                highPolicy.setName("高优先级SLA");
                highPolicy.setPriority(high);
                highPolicy.setResponseHours(4);
                highPolicy.setResolveHours(24);
                highPolicy.setAutoEscalate(true);
                highPolicy.setEscalationPriority(urgent == null ? high : urgent);
                highPolicy.setActive(true);
                slaPolicyService.createPolicy(highPolicy);
            }

            if (medium != null) {
                SlaPolicy mediumPolicy = new SlaPolicy();
                mediumPolicy.setName("中优先级SLA");
                mediumPolicy.setPriority(medium);
                mediumPolicy.setResponseHours(8);
                mediumPolicy.setResolveHours(48);
                mediumPolicy.setAutoEscalate(true);
                mediumPolicy.setEscalationPriority(high == null ? medium : high);
                mediumPolicy.setActive(true);
                slaPolicyService.createPolicy(mediumPolicy);
            }

            if (low != null) {
                SlaPolicy lowPolicy = new SlaPolicy();
                lowPolicy.setName("低优先级SLA");
                lowPolicy.setPriority(low);
                lowPolicy.setResponseHours(24);
                lowPolicy.setResolveHours(72);
                lowPolicy.setAutoEscalate(false);
                lowPolicy.setEscalationPriority(medium == null ? low : medium);
                lowPolicy.setActive(true);
                slaPolicyService.createPolicy(lowPolicy);
            }
        }

        // 初始化自动分派规则
        if (assignmentRuleService.getAllRules().isEmpty()) {
            List<User> users = userService.getAllUsers();
            User defaultAssignee = users.stream().filter(user -> "user".equalsIgnoreCase(user.getUsername())).findFirst().orElse(null);
            if (defaultAssignee != null) {
                AssignmentRule defaultRule = new AssignmentRule();
                defaultRule.setName("默认分派规则");
                defaultRule.setTargetAssignee(defaultAssignee);
                defaultRule.setPriority(100);
                defaultRule.setActive(true);
                assignmentRuleService.createRule(defaultRule);
            }
        }

        // 初始化飞书Webhook模板（默认关闭，按需填写）
        if (feishuWebhookConfigService.getAll().isEmpty()) {
            FeishuWebhookConfig template = new FeishuWebhookConfig();
            template.setName("飞书群通知模板");
            template.setWebhookUrl("https://open.feishu.cn/open-apis/bot/v2/hook/your-webhook-token");
            template.setEnabled(false);
            template.setNotifyCreate(true);
            template.setNotifyStatus(true);
            template.setNotifyComment(true);
            template.setNotifyEvaluation(true);
            template.setNotifySla(true);
            feishuWebhookConfigService.create(template);
        }
    }
}
