
# SSM 框架详细知识点指南（Spring + SpringMVC + MyBatis）

---

## 一、SSM 框架概述

- **SSM 是什么？**
  - Spring：控制反转（IoC）、面向切面编程（AOP）
  - SpringMVC：基于 Spring 的 MVC Web 框架
  - MyBatis：轻量级 ORM 框架，半自动化 SQL 映射

- **SSM 架构图**
  - 表现层：SpringMVC
  - 业务层：Spring 管理 Bean
  - 持久层：MyBatis 连接数据库

---

## 二、Spring 核心知识

- **IoC（控制反转）与 DI（依赖注入）**
  - 注解方式：`@Component`、`@Service`、`@Repository`
  - XML 配置方式

- **AOP（面向切面编程）**
  - 核心概念：切面、连接点、切入点、通知、织入
  - 实现方式：基于 XML 或 注解（`@Aspect`）

- **事务管理**
  - 编程式事务 vs 声明式事务
  - `@Transactional` 注解
  - 事务传播行为和隔离级别

- **Bean 生命周期**
  - 实例化 → 属性注入 → 初始化 → 销毁
  - `@PostConstruct`, `@PreDestroy`, `InitializingBean` 等接口

---

## 三、SpringMVC 核心知识

- **请求处理流程**
  1. DispatcherServlet 接收请求
  2. 通过 HandlerMapping 查找处理器
  3. 调用 Controller 方法
  4. 返回 ModelAndView → ViewResolver → 渲染视图

- **常用注解**
  - `@Controller`, `@RequestMapping`, `@ResponseBody`
  - `@GetMapping`, `@PostMapping`, `@RequestParam`, `@PathVariable`
  - `@RestController`（组合注解）

- **数据绑定与表单处理**
  - 对象参数接收
  - 文件上传：`MultipartFile`
  - 数据校验：JSR303（`@Valid`, `@NotNull`, `@Size`）

- **异常处理机制**
  - `@ExceptionHandler`, `@ControllerAdvice`, `HandlerExceptionResolver`

- **拦截器（Interceptor）**
  - 实现 `HandlerInterceptor` 接口：`preHandle`, `postHandle`, `afterCompletion`

---

## 四、MyBatis 核心知识

- **基本配置**
  - XML 配置文件（mybatis-config.xml）
  - Mapper XML 文件

- **SQL 映射与执行**
  - `select`, `insert`, `update`, `delete` 标签使用
  - `#{} vs ${}` 区别

- **映射机制**
  - 一对一，一对多，多对多映射
  - `resultMap` 和 `association`, `collection`

- **动态 SQL**
  - `if`, `choose`, `where`, `foreach`, `trim`

- **注解开发**
  - `@Select`, `@Insert`, `@Update`, `@Delete`

- **分页插件**
  - PageHelper 插件配置与使用

- **一级/二级缓存**
  - 一级缓存：SqlSession 级别
  - 二级缓存：Mapper 映射级别

---

## 五、SSM 整合配置

- **Spring 配置**
  - `applicationContext.xml` 或 JavaConfig
  - 组件扫描、事务管理器配置

- **SpringMVC 配置**
  - `spring-mvc.xml` 配置视图解析器、静态资源、拦截器

- **MyBatis 配置**
  - `SqlSessionFactoryBean`, `MapperScannerConfigurer`
  - 数据源（`Druid`, `HikariCP`, `C3P0`）配置

- **整合关键点**
  - 使用 `@Autowired` 注入 Mapper
  - 避免事务重复配置
  - 确保扫描包路径正确

---

## 六、常见问题与调优建议

- **乱码问题**
  - 配置过滤器 `CharacterEncodingFilter`

- **事务不生效**
  - 是否开启 `@EnableTransactionManagement`
  - 方法是否为 public 且通过代理调用

- **MyBatis 返回 null**
  - Mapper.xml 路径是否正确
  - SQL 查询条件是否正确

- **N+1 查询问题**
  - 使用懒加载或优化 SQL

- **性能优化建议**
  - 开启 SQL 日志输出
  - 适当使用缓存（EhCache、Redis）
  - SQL调优与索引优化

---

## 七、项目实战建议

- **模块分层结构**
  - controller → service → dao → entity

- **常用中间件整合**
  - Redis、RabbitMQ、Elasticsearch

- **前后端分离实践**
  - 接口返回 JSON，统一封装结果类
  - 使用 Swagger 生成接口文档

- **代码规范与开发建议**
  - 使用统一异常处理机制
  - 引入日志框架（SLF4J + Logback）
  - 单元测试（JUnit + Mockito）

---

## 推荐资料

- 《Spring 实战（第5版）》
- 《MyBatis 从入门到精通》
- 《轻量级Java EE企业应用实战（SSM整合）》
