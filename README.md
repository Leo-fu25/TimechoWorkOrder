# TimechoWorkOrder 工单系统

## 项目简介
TimechoWorkOrder是一个基于Spring Boot的工单管理系统，提供完整的工单生命周期管理功能，包括工单创建、分配、处理、跟踪和关闭等流程。系统支持多维度的工单管理，包括工单类型、优先级、状态等管理，以及工单评论、附件、需求评估等功能。

## 技术栈
- **后端框架**：Spring Boot 2.5.4
- **数据库**：MySQL
- **ORM**：Spring Data JPA
- **安全**：Spring Security
- **构建工具**：Maven
- **前端**：支持任何前端框架（如React、Vue、Angular等）

## 项目结构
```
TimechoWorkOrder/
├── src/
│   ├── main/
│   │   ├── java/com/timecho/workorder/
│   │   │   ├── config/             # 配置类，包含安全配置和数据初始化
│   │   │   ├── controller/         # 控制器层，处理HTTP请求
│   │   │   ├── dto/                # 数据传输对象
│   │   │   ├── exception/          # 异常处理
│   │   │   ├── model/              # 模型层，定义数据实体
│   │   │   ├── repository/         # 仓库层，处理数据持久化
│   │   │   ├── service/            # 服务层，实现业务逻辑
│   │   │   └── WorkOrderApplication.java  # 应用程序入口
│   │   └── resources/
│   │       └── application.properties  # 应用程序配置
│   └── test/
├── pom.xml                        # Maven配置文件
└── README.md                      # 项目说明文档
```

## 核心功能

### 1. 工单管理
- **创建工单**：支持填写工单标题、描述、类型、来源、客户信息、产品名称、影响范围、标签等详细信息
- **查询工单**：支持按关键词、请求人、处理人、部门、状态、优先级、类型、来源、客户类型、产品名称、标签、是否逾期等多维度查询
- **更新工单**：支持更新工单的基本信息
- **分配工单**：支持将工单分配给指定处理人
- **状态管理**：支持更新工单状态，包括批量更新状态
- **工单评论**：支持添加和查看工单评论，包括内部评论和公开评论
- **工单附件**：支持上传和查看工单附件
- **需求评估**：支持对工单进行需求评估，包括需求价值、开发工作量、客户权重、竞品影响、影响范围等维度
- **工单历史**：记录工单的所有操作历史，便于追溯
- **工单统计**：提供工单统计信息，包括总数、待处理、处理中、已完成、逾期、响应超时等指标

### 2. 用户管理
- **用户创建**：支持创建新用户，设置用户名、密码、邮箱等信息
- **用户查询**：支持按用户名、邮箱、部门等查询用户
- **用户更新**：支持更新用户信息，包括密码重置
- **用户删除**：支持删除用户

### 3. 部门管理
- **部门创建**：支持创建新部门，设置部门名称和描述
- **部门查询**：支持查询所有部门
- **部门更新**：支持更新部门信息
- **部门删除**：支持删除部门

### 4. 状态管理
- **状态创建**：支持创建新工单状态
- **状态查询**：支持查询所有状态
- **状态更新**：支持更新状态信息
- **状态删除**：支持删除状态

### 5. 优先级管理
- **优先级创建**：支持创建新工单优先级，设置优先级级别
- **优先级查询**：支持查询所有优先级
- **优先级更新**：支持更新优先级信息
- **优先级删除**：支持删除优先级

### 6. 工单类型管理
- **类型创建**：支持创建新工单类型，如需求、缺陷、咨询、故障等
- **类型查询**：支持查询所有工单类型
- **类型更新**：支持更新工单类型信息
- **类型删除**：支持删除工单类型

## 安装与运行

### 安装步骤
1. **克隆项目**
   ```bash
   git clone <项目地址>
   cd TimechoWorkOrder
   ```

2. **构建项目**
   ```bash
   mvn clean package
   ```

3. **运行项目**
   ```bash
   java -jar target/workorder-1.0.0.jar
   ```

### 运行说明
- 应用程序默认在端口8080上运行
- H2数据库控制台地址：http://localhost:8080/h2-console
  - JDBC URL：jdbc:h2:mem:workorder_db
  - 用户名：sa
  - 密码：（空）

## API接口文档

### 工单管理
- `GET /api/workorders` - 获取所有工单（支持多维度查询）
- `POST /api/workorders` - 创建新工单
- `GET /api/workorders/statistics` - 获取工单统计信息
- `GET /api/workorders/{id}` - 获取指定工单
- `PUT /api/workorders/{id}` - 更新指定工单
- `PATCH /api/workorders/{id}/assignee` - 分配工单给指定处理人
- `PATCH /api/workorders/{id}/status` - 更新工单状态
- `POST /api/workorders/batch/status` - 批量更新工单状态
- `POST /api/workorders/{id}/comments` - 添加工单评论
- `GET /api/workorders/{id}/comments` - 获取工单评论
- `POST /api/workorders/{id}/attachments` - 添加工单附件
- `GET /api/workorders/{id}/attachments` - 获取工单附件
- `POST /api/workorders/{id}/evaluations` - 添加工单需求评估
- `GET /api/workorders/{id}/evaluations` - 获取工单需求评估
- `DELETE /api/workorders/{id}` - 删除指定工单
- `GET /api/workorders/{id}/history` - 获取工单历史记录
- `GET /api/workorders/requester/{requesterId}` - 获取指定请求者的工单
- `GET /api/workorders/assignee/{assigneeId}` - 获取指定处理者的工单
- `GET /api/workorders/department/{departmentId}` - 获取指定部门的工单

### 用户管理
- `GET /api/users` - 获取所有用户
- `POST /api/users` - 创建新用户
- `GET /api/users/{id}` - 获取指定用户
- `PUT /api/users/{id}` - 更新指定用户
- `DELETE /api/users/{id}` - 删除指定用户
- `GET /api/users/username/{username}` - 根据用户名获取用户
- `GET /api/users/email/{email}` - 根据邮箱获取用户
- `GET /api/users/department/{departmentId}` - 获取指定部门的用户

### 部门管理
- `GET /api/departments` - 获取所有部门
- `POST /api/departments` - 创建新部门
- `GET /api/departments/{id}` - 获取指定部门
- `PUT /api/departments/{id}` - 更新指定部门
- `DELETE /api/departments/{id}` - 删除指定部门

### 状态管理
- `GET /api/statuses` - 获取所有状态
- `POST /api/statuses` - 创建新状态
- `GET /api/statuses/{id}` - 获取指定状态
- `PUT /api/statuses/{id}` - 更新指定状态
- `DELETE /api/statuses/{id}` - 删除指定状态

### 优先级管理
- `GET /api/priorities` - 获取所有优先级
- `POST /api/priorities` - 创建新优先级
- `GET /api/priorities/{id}` - 获取指定优先级
- `PUT /api/priorities/{id}` - 更新指定优先级
- `DELETE /api/priorities/{id}` - 删除指定优先级

### 工单类型管理
- `GET /api/types` - 获取所有工单类型
- `POST /api/types` - 创建新工单类型
- `GET /api/types/{id}` - 获取指定工单类型
- `PUT /api/types/{id}` - 更新指定工单类型
- `DELETE /api/types/{id}` - 删除指定工单类型

## 数据库配置
系统默认使用H2内存数据库，配置信息如下：

```properties
# 服务器配置
server.port=8080

# 数据库配置
spring.datasource.url=jdbc:h2:mem:workorder_db
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA配置
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect

# H2控制台配置
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# 日志配置
logging.level.org.springframework=INFO
logging.level.com.timecho.workorder=DEBUG
```

## 初始数据
系统启动时会自动初始化以下基础数据：

### 部门
- IT部门：负责信息技术相关工作
- 人力资源部：负责人力资源管理工作
- 财务部：负责财务管理工作

### 状态
- PENDING：待处理
- IN_PROGRESS：处理中
- COMPLETED：已完成
- CANCELLED：已取消

### 优先级
- LOW：低优先级（1）
- MEDIUM：中优先级（2）
- HIGH：高优先级（3）
- URGENT：紧急（4）

### 工单类型
- DEMAND：需求
- BUG：缺陷
- CONSULTATION：咨询
- INCIDENT：故障

### 用户
- 管理员账号：username=admin, password=admin123
- 普通用户账号：username=user, password=user123

## 安全配置
系统使用Spring Security进行安全管理，主要配置包括：

- 关闭CSRF保护，方便API调用
- 允许H2控制台在iframe中运行
- 允许所有API接口的访问
- 使用BCryptPasswordEncoder进行密码加密

## 技术特性
1. **事务管理**：使用@Transactional注解确保数据操作的原子性
2. **乐观锁**：使用@Retryable注解处理并发冲突
3. **数据验证**：使用@Valid注解验证请求参数
4. **异常处理**：提供全局异常处理机制
5. **统计功能**：提供工单统计信息
6. **需求评估**：支持对工单进行多维度评估
7. **工单历史**：详细记录工单的所有操作历史
8. **批量操作**：支持批量更新工单状态

## 注意事项
1. **数据库配置**：本系统默认使用MySQL数据库，连接到localhost:3306/timecho_workorder。首次运行时会自动创建数据库（如果不存在）。

2. **生产环境配置**：
   - 开启CSRF保护
   - 配置细粒度的权限控制
   - 使用数据库认证替代内存认证
   - 配置HTTPS
   - 修改数据库连接信息，使用生产环境的数据库地址和凭据

3. **性能优化**：
   - 对于大规模应用，建议使用缓存机制
   - 优化数据库查询，添加适当的索引
   - 考虑使用异步处理处理大量工单
   - 配置数据库连接池

4. **扩展建议**：
   - 添加邮件通知功能
   - 实现工单模板功能
   - 集成第三方系统（如OA、CRM等）
   - 添加报表和统计功能
   - 实现SLA（服务水平协议）管理

## 联系我们
如有任何问题或建议，请联系我们。

---

**TimechoWorkOrder工单系统** - 提供高效、便捷的工单管理解决方案