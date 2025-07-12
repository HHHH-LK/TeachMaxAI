# SSM框架详细知识图谱

## 1. Spring框架核心

### 1.1 IoC容器 (控制反转)
- **概念**: 对象创建控制权的转移
- **实现方式**: 
  - 基于XML配置
  - 基于注解配置
  - 基于Java配置类
- **容器类型**:
  - BeanFactory: 基础容器
  - ApplicationContext: 高级容器
- **依赖注入方式**:
  - 构造器注入
  - Setter注入
  - 字段注入
- **Bean作用域**:
  - singleton (默认)
  - prototype
  - request
  - session
  - application

### 1.2 AOP (面向切面编程)
- **核心概念**:
  - 切面 (Aspect)
  - 连接点 (Join Point)
  - 切点 (Pointcut)
  - 通知 (Advice)
  - 织入 (Weaving)
- **通知类型**:
  - @Before: 前置通知
  - @After: 后置通知
  - @AfterReturning: 返回后通知
  - @AfterThrowing: 异常通知
  - @Around: 环绕通知
- **切点表达式**:
  - execution(): 方法执行切点
  - within(): 类型切点
  - args(): 参数切点
  - @annotation(): 注解切点

### 1.3 Spring注解体系
- **组件注解**:
  - @Component: 通用组件
  - @Service: 业务层组件
  - @Repository: 数据访问层组件
  - @Controller: 表现层组件
- **配置注解**:
  - @Configuration: 配置类
  - @Bean: Bean定义
  - @ComponentScan: 组件扫描
  - @PropertySource: 属性源
- **依赖注入注解**:
  - @Autowired: 自动装配
  - @Resource: 资源注入
  - @Value: 值注入
  - @Qualifier: 限定符

## 2. Spring MVC框架

### 2.1 MVC架构模式
- **Model**: 数据模型
- **View**: 视图层
- **Controller**: 控制器层
- **工作流程**:
  1. 请求到达DispatcherServlet
  2. HandlerMapping查找处理器
  3. HandlerAdapter调用处理器
  4. ViewResolver解析视图
  5. 渲染视图返回响应

### 2.2 核心组件
- **DispatcherServlet**: 前端控制器
- **HandlerMapping**: 处理器映射器
- **HandlerAdapter**: 处理器适配器
- **ViewResolver**: 视图解析器
- **HandlerInterceptor**: 拦截器

### 2.3 常用注解
- **请求映射**:
  - @RequestMapping: 通用请求映射
  - @GetMapping: GET请求映射
  - @PostMapping: POST请求映射
  - @PutMapping: PUT请求映射
  - @DeleteMapping: DELETE请求映射
- **参数绑定**:
  - @RequestParam: 请求参数
  - @PathVariable: 路径变量
  - @RequestBody: 请求体
  - @RequestHeader: 请求头
  - @CookieValue: Cookie值
- **响应处理**:
  - @ResponseBody: 响应体
  - @RestController: REST控制器
  - @ModelAttribute: 模型属性

### 2.4 数据绑定与验证
- **数据绑定**:
  - 自动类型转换
  - 自定义类型转换器
  - 数据格式化
- **数据验证**:
  - JSR-303/JSR-349规范
  - @Valid注解
  - 常用验证注解:
    - @NotNull
    - @NotEmpty
    - @NotBlank
    - @Size
    - @Min/@Max
    - @Pattern
    - @Email

## 3. MyBatis框架

### 3.1 核心概念
- **SqlSession**: 会话对象
- **SqlSessionFactory**: 会话工厂
- **Mapper**: 映射器接口
- **Configuration**: 配置对象

### 3.2 配置文件
- **mybatis-config.xml**: 全局配置
  - environments: 环境配置
  - typeAliases: 类型别名
  - plugins: 插件配置
  - mappers: 映射器配置
- **Mapper.xml**: 映射文件
  - select: 查询语句
  - insert: 插入语句
  - update: 更新语句
  - delete: 删除语句
  - resultMap: 结果映射

### 3.3 动态SQL
- **if**: 条件判断
- **choose/when/otherwise**: 多条件选择
- **trim/where/set**: 条件处理
- **foreach**: 循环处理
- **bind**: 变量绑定

### 3.4 高级特性
- **缓存机制**:
  - 一级缓存 (SqlSession级别)
  - 二级缓存 (Mapper级别)
  - 自定义缓存
- **延迟加载**:
  - lazyLoadingEnabled
  - aggressiveLazyLoading
- **插件机制**:
  - Interceptor接口
  - @Intercepts注解
  - 分页插件PageHelper

## 4. SSM整合配置

### 4.1 Spring与MyBatis整合
- **依赖配置**:
  - mybatis-spring
  - 数据源配置
  - 事务管理器
- **关键配置**:
  - SqlSessionFactoryBean
  - MapperScannerConfigurer
  - @MapperScan注解

### 4.2 Spring MVC配置
- **web.xml配置**:
  - DispatcherServlet
  - ContextLoaderListener
  - 字符编码过滤器
- **Spring配置**:
  - 组件扫描
  - 视图解析器
  - 静态资源处理

### 4.3 事务管理
- **事务注解**:
  - @Transactional
  - @EnableTransactionManagement
- **事务属性**:
  - propagation: 传播行为
  - isolation: 隔离级别
  - timeout: 超时时间
  - readOnly: 只读标识
  - rollbackFor: 回滚异常

## 5. 常见错误及解决方案

### 5.1 配置错误
- **Bean未找到**: 检查组件扫描路径
- **循环依赖**: 使用@Lazy或重构代码
- **配置文件路径错误**: 检查classpath路径
- **数据源配置错误**: 检查连接参数

### 5.2 注解使用错误
- **@Autowired失效**: 确保在Spring容器中
- **@Transactional失效**: 避免同类调用
- **@RequestMapping重复**: 检查映射路径唯一性
- **@ResponseBody缺失**: JSON响应必须添加

### 5.3 MyBatis错误
- **Mapper找不到**: 检查扫描配置
- **SQL语法错误**: 检查动态SQL拼接
- **类型转换错误**: 配置TypeHandler
- **缓存问题**: 清理缓存或关闭缓存

### 5.4 Web层错误
- **404错误**: 检查请求路径映射
- **405错误**: 检查请求方法匹配
- **参数绑定失败**: 检查参数名称
- **中文乱码**: 配置字符编码过滤器

## 6. 最佳实践

### 6.1 分层架构
- **Controller层**: 处理请求响应
- **Service层**: 业务逻辑处理
- **Dao层**: 数据访问处理
- **Entity层**: 实体对象定义

### 6.2 代码规范
- **命名规范**: 包名、类名、方法名
- **注释规范**: 类注释、方法注释
- **异常处理**: 统一异常处理
- **日志记录**: 合理使用日志级别

### 6.3 性能优化
- **连接池配置**: 合理配置连接池参数
- **缓存使用**: 合理使用各级缓存
- **SQL优化**: 避免N+1查询
- **分页查询**: 使用物理分页

### 6.4 安全考虑
- **SQL注入防护**: 使用预编译语句
- **XSS防护**: 输出转义处理
- **CSRF防护**: 使用CSRF令牌
- **权限控制**: 实现认证授权

## 7. 测试策略

### 7.1 单元测试
- **JUnit**: 基础测试框架
- **Mockito**: Mock对象框架
- **Spring Test**: Spring测试支持
- **@SpringBootTest**: 集成测试注解

### 7.2 集成测试
- **@WebMvcTest**: Web层测试
- **@DataJpaTest**: 数据层测试
- **@TestConfiguration**: 测试配置
- **TestRestTemplate**: REST测试模板

## 8. 问题诊断工具

### 8.1 日志分析
- **Log4j/Logback**: 日志框架
- **MDC**: 日志上下文
- **日志级别**: DEBUG、INFO、WARN、ERROR

### 8.2 性能监控
- **JProfiler**: 性能分析工具
- **VisualVM**: JVM监控工具
- **Spring Boot Actuator**: 应用监控
- **Micrometer**: 指标收集