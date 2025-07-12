
# Java 进阶知识点详细指南

---

## 一、面向对象深入理解（OOP）

- **继承、封装、多态**
  - 方法重载与重写（Override vs Overload）
  - `super` 和 `this` 的区别和使用场景
  - 抽象类 vs 接口（interface）
  - 接口默认方法（Java 8+）

- **对象的创建与初始化顺序**
  - 构造函数、静态块、实例块的执行顺序
  - 类加载过程（加载、验证、准备、解析、初始化）

- **对象克隆**
  - 浅拷贝 vs 深拷贝
  - 实现 `Cloneable` 接口 vs 使用序列化

---

## 二、集合框架深入（Java Collection Framework）

- **常见集合的底层原理**
  - `ArrayList`：动态数组，线程不安全，默认容量10
  - `LinkedList`：双向链表结构
  - `HashMap`：数组 + 链表 + 红黑树（JDK 1.8+）
  - `HashSet`：基于 `HashMap` 实现
  - `ConcurrentHashMap`：分段锁（JDK 1.7）→ CAS+Node数组（JDK 1.8）

- **集合类比较**
  - `List` vs `Set` vs `Map`
  - `TreeMap` / `TreeSet`：基于红黑树实现
  - `LinkedHashMap`：保持插入顺序

- **Fail-Fast 与 Fail-Safe**
  - ConcurrentModificationException 原理

---

## 三、异常处理机制

- **Checked vs Unchecked 异常**
- **自定义异常类的设计**
- **try-with-resources（Java 7+）**

---

## 四、泛型（Generics）

- **通配符**
  - `<?>`, `<? extends T>`, `<? super T>`

- **泛型擦除与类型边界**
  - 编译后泛型信息被擦除
  - 类型安全与运行时检查

---

## 五、注解（Annotations）

- **内置注解**
  - `@Override`, `@Deprecated`, `@SuppressWarnings`

- **自定义注解**
  - 元注解：`@Target`, `@Retention`, `@Documented`, `@Inherited`

- **注解处理器（APT）与反射结合使用**

---

## 六、反射与动态代理

- **反射 API**
  - `Class`对象、构造器、字段、方法的使用
  - 修改私有属性与方法

- **动态代理**
  - JDK 动态代理（基于接口）
  - CGLIB 动态代理（基于继承）

- **实际应用**
  - AOP、日志记录、权限控制、缓存代理等

---

## 七、Java 并发编程

- **线程基础**
  - `Thread`类 vs `Runnable`接口
  - 线程状态生命周期

- **线程安全**
  - `volatile`、`synchronized`、`Lock`

- **并发工具类（JUC）**
  - `CountDownLatch`, `CyclicBarrier`, `Semaphore`, `Exchanger`
  - 线程池：`ExecutorService`, `ThreadPoolExecutor`
  - `Callable` + `Future` + `FutureTask`

- **原子类**
  - `AtomicInteger`, `AtomicReference` 等

- **AQS（AbstractQueuedSynchronizer）**

---

## 八、JVM 内部机制

- **内存结构**
  - 堆、栈、本地方法区、方法区、程序计数器

- **垃圾回收（GC）**
  - Minor GC vs Full GC
  - 算法：标记-清除、复制、标记-整理、分代
  - 收集器：Serial、ParNew、CMS、G1、ZGC

- **类加载机制**
  - 双亲委派模型
  - 自定义类加载器

- **性能监控工具**
  - jstat、jmap、jstack、jconsole、VisualVM、JMC

---

## 九、Java 新特性（JDK 8+）

- **Lambda 表达式 与 Stream API**
  - 函数式接口、方法引用、函数组合
  - 中间操作 vs 终止操作

- **Optional 类**
  - 避免空指针异常

- **时间日期 API（java.time）**

- **模块系统（JDK 9+）**
  - `module-info.java`

- **JDK 14+ 特性**
  - Records、Switch 表达式、Text Blocks 等

---

## 十、常用框架与技术栈（简略）

- **Spring / Spring Boot / Spring Cloud**
  - IOC / AOP、自动装配、Bean 生命周期、REST 接口开发
  - 多环境配置、服务注册与调用（Eureka、Feign）

- **MyBatis**
  - XML映射、注解式SQL、缓存机制

- **Hibernate / JPA**
  - ORM、懒加载、事务管理

---

## 十一、网络编程

- **Socket 编程**
  - BIO、NIO、AIO

- **Netty 框架**
  - Reactor 模型、EventLoopGroup

---

## 十二、设计模式（建议掌握常用的 10~15 种）

- **创建型模式**
  - 单例、工厂、建造者、原型

- **结构型模式**
  - 适配器、代理、装饰器、桥接、外观、组合、享元

- **行为型模式**
  - 策略、模板方法、观察者、责任链、状态、命令

---

## 学习建议

- 推荐阅读：
  - 《Effective Java》
  - 《Java 并发编程实战》
  - 《深入理解Java虚拟机》

- 多做项目实践：
  - 电商系统、博客平台、爬虫工具等

- 工具熟练使用：
  - IDE 调试、日志分析、监控工具等

---
