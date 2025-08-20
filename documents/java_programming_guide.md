# Java程序设计知识点大全

## 1. Java基础语法

### 1.1 数据类型
Java中的数据类型分为基本数据类型和引用数据类型。基本数据类型包括byte（8位）、short（16位）、int（32位）、long（64位）、float（32位）、double（64位）、char（16位）、boolean。引用数据类型包括类、接口、数组等。基本数据类型存储在栈内存中，引用数据类型的对象存储在堆内存中，栈中存储对象的引用地址。

### 1.2 变量和常量
变量是程序中可以改变的数据存储单元，必须先声明后使用。常量使用final关键字修饰，一旦赋值不能修改。变量有局部变量、成员变量和静态变量三种类型。局部变量在方法内定义，作用域仅限于方法内部。成员变量属于对象，每个对象都有自己的成员变量副本。静态变量属于类，所有对象共享同一个静态变量。

### 1.3 运算符
Java提供了丰富的运算符，包括算术运算符（+、-、*、/、%）、关系运算符（==、!=、>、<、>=、<=）、逻辑运算符（&&、||、!）、位运算符（&、|、^、~、<<、>>、>>>）、赋值运算符（=、+=、-=等）、条件运算符（? :）。运算符有优先级规则，括号具有最高优先级。

### 1.4 控制结构
Java提供了三种基本控制结构：顺序结构、选择结构和循环结构。选择结构包括if-else语句和switch语句。循环结构包括for循环、while循环和do-while循环。此外还有增强for循环（for-each）用于遍历集合和数组。break语句用于跳出循环，continue语句用于跳过当前循环的剩余部分。

## 2. 面向对象编程

### 2.1 类和对象
类是对象的模板或蓝图，定义了对象的属性和行为。对象是类的实例，通过new关键字创建。类包含成员变量（属性）和成员方法（行为）。构造方法是特殊的方法，用于初始化对象，方法名与类名相同且没有返回值类型。可以重载构造方法以提供不同的初始化方式。

### 2.2 封装
封装是面向对象编程的基本特征之一，通过访问修饰符控制成员的可见性。Java提供了四种访问修饰符：private（仅在本类内可访问）、default（包访问权限）、protected（包内和子类可访问）、public（所有地方都可访问）。良好的封装实践是将成员变量设为private，通过getter和setter方法访问。

### 2.3 继承
继承允许一个类获得另一个类的属性和方法，实现代码重用。Java使用extends关键字实现继承，子类可以继承父类的非私有成员。Java只支持单继承，但可以通过接口实现多重继承的效果。子类可以重写父类的方法，使用@Override注解确保正确重写。super关键字用于访问父类的成员。

### 2.4 多态
多态是指同一个引用类型在不同情况下的多种状态。Java通过方法重载和方法重写实现多态。方法重载是在同一个类中定义多个同名但参数不同的方法。方法重写是子类重新定义父类的方法。运行时多态通过动态绑定实现，根据对象的实际类型调用相应的方法。

### 2.5 抽象类和接口
抽象类使用abstract关键字定义，不能被实例化，可以包含抽象方法和具体方法。抽象方法没有方法体，必须在子类中实现。接口使用interface关键字定义，所有方法默认为public abstract，所有变量默认为public static final。类可以实现多个接口，接口可以继承多个接口。Java 8开始，接口可以包含默认方法和静态方法。

## 3. 异常处理

### 3.1 异常层次结构
Java异常继承层次的根类是Throwable，其下有两个子类：Error和Exception。Error表示系统级错误，通常不由程序处理。Exception分为检查异常（Checked Exception）和运行时异常（Runtime Exception）。检查异常必须在编译时处理，运行时异常可以不显式处理。常见的运行时异常包括NullPointerException、ArrayIndexOutOfBoundsException等。

### 3.2 异常处理机制
Java使用try-catch-finally语句处理异常。try块包含可能抛出异常的代码，catch块处理特定类型的异常，finally块包含无论是否发生异常都要执行的代码。可以有多个catch块处理不同类型的异常，catch块的顺序应该从具体到抽象。throws关键字用于方法声明中，表示方法可能抛出的异常。throw关键字用于主动抛出异常。

### 3.3 自定义异常
可以通过继承Exception类或其子类创建自定义异常。自定义异常应该提供有意义的异常信息，通常重写toString()方法。如果继承RuntimeException，则为运行时异常；如果继承Exception，则为检查异常。自定义异常应该遵循命名规范，以Exception结尾。

## 4. 集合框架

### 4.1 Collection接口
Collection是Java集合框架的根接口，定义了集合的基本操作方法。主要子接口包括List、Set和Queue。List允许重复元素且有序，主要实现类有ArrayList、LinkedList、Vector。Set不允许重复元素，主要实现类有HashSet、TreeSet、LinkedHashSet。Queue表示队列，主要实现类有LinkedList、PriorityQueue、ArrayDeque。

### 4.2 Map接口
Map表示键值对的映射关系，键不能重复但值可以重复。主要实现类包括HashMap、TreeMap、LinkedHashMap、Hashtable。HashMap基于哈希表实现，查找效率高但不保证顺序。TreeMap基于红黑树实现，键按自然顺序或自定义顺序排序。LinkedHashMap保持插入顺序或访问顺序。Hashtable是线程安全的，但性能较低。

### 4.3 集合选择和性能
ArrayList适合随机访问和遍历，LinkedList适合频繁插入删除。HashSet基于HashMap实现，查找效率高。TreeSet基于TreeMap实现，元素有序但性能略低。HashMap查找、插入、删除的平均时间复杂度为O(1)。TreeMap的查找、插入、删除时间复杂度为O(log n)。选择集合时要考虑数据规模、操作类型、性能要求和线程安全性。

### 4.4 迭代器和增强for循环
迭代器（Iterator）提供了统一的集合遍历方式，支持hasNext()、next()和remove()方法。ListIterator是Iterator的子接口，支持双向遍历和修改操作。增强for循环（for-each）是迭代器的语法糖，代码更简洁但功能有限。在遍历过程中修改集合可能导致ConcurrentModificationException，应使用迭代器的remove()方法。

## 5. 多线程编程

### 5.1 线程创建和启动
Java提供两种创建线程的方式：继承Thread类和实现Runnable接口。继承Thread类简单但限制了类的继承。实现Runnable接口更灵活，支持多重继承。线程通过start()方法启动，不能直接调用run()方法。每个线程只能启动一次，重复启动会抛出IllegalThreadStateException。

### 5.2 线程状态和生命周期
线程有六种状态：NEW（新建）、RUNNABLE（可运行）、BLOCKED（阻塞）、WAITING（等待）、TIMED_WAITING（限时等待）、TERMINATED（终止）。线程状态转换通过特定方法触发，如start()、wait()、notify()、sleep()、join()等。理解线程状态转换对于多线程编程和问题诊断非常重要。

### 5.3 线程同步
多线程访问共享资源可能导致数据不一致，需要使用同步机制保证线程安全。synchronized关键字可以修饰方法或代码块，实现互斥访问。volatile关键字保证变量的可见性和有序性。Lock接口提供了比synchronized更灵活的锁机制，支持公平锁、可中断锁、条件锁等特性。

### 5.4 线程间通信
线程间通信主要通过wait()、notify()、notifyAll()方法实现，这些方法必须在synchronized块中调用。生产者-消费者模式是典型的线程间通信场景。CountDownLatch、CyclicBarrier、Semaphore等工具类提供了更高级的线程协调机制。BlockingQueue简化了生产者-消费者模式的实现。

### 5.5 线程池
线程池管理和重用线程，避免频繁创建销毁线程的开销。ExecutorService是线程池的核心接口，ThreadPoolExecutor是主要实现类。Executors工具类提供了创建常用线程池的静态方法，如newFixedThreadPool()、newCachedThreadPool()、newSingleThreadExecutor()。线程池参数包括核心线程数、最大线程数、空闲时间、任务队列等。

## 6. I/O流操作

### 6.1 字节流和字符流
Java I/O流分为字节流和字符流两大类。字节流处理8位字节数据，适合处理二进制文件，主要类包括InputStream、OutputStream及其子类。字符流处理16位字符数据，适合处理文本文件，主要类包括Reader、Writer及其子类。字符流内部使用字节流和字符编码转换实现。

### 6.2 文件操作
File类表示文件和目录的抽象路径，提供了文件操作的基本方法，如exists()、createNewFile()、delete()、mkdir()等。FileInputStream和FileOutputStream用于文件字节流操作。FileReader和FileWriter用于文件字符流操作。BufferedInputStream、BufferedOutputStream、BufferedReader、BufferedWriter提供缓冲功能，提高I/O效率。

### 6.3 序列化和反序列化
序列化是将对象转换为字节序列的过程，反序列化是相反过程。实现Serializable接口的类可以被序列化。ObjectOutputStream用于序列化对象，ObjectInputStream用于反序列化对象。serialVersionUID用于版本控制，transient关键字修饰的字段不会被序列化。序列化常用于对象持久化和网络传输。

### 6.4 NIO（New I/O）
NIO提供了基于通道和缓冲区的I/O操作方式，支持非阻塞I/O。Channel表示数据源和目标之间的连接，Buffer是数据容器。Selector实现多路复用，可以监控多个Channel的状态。NIO适合处理大量连接的网络应用，提高了I/O性能和系统吞吐量。

## 7. 网络编程

### 7.1 TCP编程
TCP是面向连接的可靠传输协议，Java通过Socket和ServerSocket实现TCP编程。ServerSocket监听客户端连接请求，accept()方法返回与客户端通信的Socket。Socket表示客户端连接，通过getInputStream()和getOutputStream()获取输入输出流。TCP编程需要处理连接建立、数据传输和连接关闭等环节。

### 7.2 UDP编程
UDP是无连接的不可靠传输协议，Java通过DatagramSocket和DatagramPacket实现UDP编程。DatagramPacket封装要发送或接收的数据，包含数据、目标地址和端口信息。DatagramSocket用于发送和接收数据包。UDP编程简单高效，适合实时性要求高但可以容忍数据丢失的应用。

### 7.3 URL和URLConnection
URL类表示统一资源定位符，可以方便地访问网络资源。URLConnection表示与URL指向资源的连接，可以读取资源内容和获取资源信息。HttpURLConnection是URLConnection的子类，专门用于HTTP连接，支持GET、POST等HTTP方法。这些类简化了网络资源访问的编程工作。

## 8. 数据库编程

### 8.1 JDBC基础
JDBC（Java Database Connectivity）是Java访问数据库的标准API。JDBC驱动负责与具体数据库通信，分为Type 1到Type 4四种类型。DriverManager管理数据库驱动，Connection表示数据库连接，Statement用于执行SQL语句，ResultSet表示查询结果集。

### 8.2 数据库操作
Statement接口有三个主要实现：Statement、PreparedStatement、CallableStatement。Statement用于执行简单SQL语句，PreparedStatement用于执行预编译SQL语句，可以防止SQL注入攻击。CallableStatement用于调用存储过程。executeQuery()执行查询语句，executeUpdate()执行更新语句，execute()执行任意SQL语句。

### 8.3 事务处理
事务具有ACID特性：原子性、一致性、隔离性、持久性。JDBC默认自动提交事务，可以通过setAutoCommit(false)关闭自动提交。手动事务需要显式调用commit()提交或rollback()回滚。Connection接口提供了设置事务隔离级别的方法，包括READ_UNCOMMITTED、READ_COMMITTED、REPEATABLE_READ、SERIALIZABLE。

### 8.4 连接池
数据库连接是昂贵资源，连接池通过重用连接提高性能。常用连接池包括DBCP、C3P0、HikariCP等。连接池配置参数包括初始连接数、最大连接数、最小空闲连接数、连接超时时间等。连接池还提供连接验证、连接回收、性能监控等功能。

## 9. 反射机制

### 9.1 反射概念
反射是Java提供的动态获取类信息和动态调用类方法的机制。通过反射可以在运行时获取类的构造方法、字段、方法等信息，并可以创建对象、访问字段、调用方法。反射的核心类包括Class、Constructor、Field、Method等。

### 9.2 Class类
Class类表示正在运行的Java类或接口，每个类都有一个对应的Class对象。获取Class对象有三种方式：类名.class、对象.getClass()、Class.forName()。Class类提供了获取类信息的方法，如getName()、getSuperclass()、getInterfaces()、getConstructors()、getFields()、getMethods()等。

### 9.3 动态创建对象
通过Constructor对象可以动态创建类的实例。Constructor.newInstance()方法根据构造方法参数创建对象。如果类有无参构造方法，也可以使用Class.newInstance()方法创建对象。动态创建对象常用于工厂模式、依赖注入等设计模式的实现。

### 9.4 动态调用方法
通过Method对象可以动态调用类的方法。Method.invoke()方法需要传入对象实例和方法参数。对于静态方法，对象实例可以传入null。setAccessible(true)可以访问私有方法。反射调用方法的性能比直接调用要低，应当谨慎使用。

## 10. 注解

### 10.1 注解基础
注解（Annotation）是Java提供的元数据机制，可以为程序元素添加元信息。注解不影响程序运行，但可以被工具、框架、编译器等处理。Java内置了常用注解，如@Override、@Deprecated、@SuppressWarnings等。注解使用@interface关键字定义。

### 10.2 元注解
元注解是用来注解其他注解的注解，Java提供了四个元注解：@Target指定注解的使用目标，@Retention指定注解的保留策略，@Documented指示注解应该被javadoc工具记录，@Inherited指示注解可以被子类继承。

### 10.3 自定义注解
自定义注解可以包含属性，属性类型限制为基本类型、String、Class、枚举、注解、数组。属性可以有默认值，使用default关键字指定。注解处理通过反射实现，在运行时获取注解信息并进行相应处理。

### 10.4 注解应用
注解广泛应用于框架开发中，如Spring的@Autowired、@Service、@Controller，JPA的@Entity、@Table、@Column等。注解简化了配置，提高了代码的可读性和可维护性。注解处理器可以在编译时处理注解，生成代码或进行检查。

## 11. 泛型

### 11.1 泛型概念
泛型（Generics）提供了编译时类型安全检查机制，允许在定义类、接口、方法时使用类型参数。泛型消除了类型转换的需要，减少了ClassCastException的风险。泛型信息只在编译时存在，运行时会被擦除（类型擦除）。

### 11.2 泛型类和泛型接口
泛型类在类名后用尖括号声明类型参数，如class Box<T>。类型参数可以用作字段类型、方法参数类型、返回值类型等。泛型接口类似泛型类，如interface List<E>。类型参数可以有边界，如<T extends Number>表示T必须是Number的子类。

### 11.3 泛型方法
泛型方法在返回值类型前声明类型参数，如public <T> T method(T param)。泛型方法的类型参数与类的类型参数独立。静态方法不能使用类的类型参数，但可以声明自己的类型参数。泛型方法可以推断类型参数，通常不需要显式指定。

### 11.4 通配符
通配符（?）表示未知类型，有三种形式：无边界通配符（?）、上边界通配符（? extends T）、下边界通配符（? super T）。上边界通配符用于读取操作，下边界通配符用于写入操作。PECS原则：Producer extends, Consumer super，指导通配符的使用。

## 12. Lambda表达式和Stream API

### 12.1 函数式接口
函数式接口是只有一个抽象方法的接口，可以使用@FunctionalInterface注解标识。Java 8提供了常用的函数式接口，如Predicate、Function、Consumer、Supplier等。函数式接口是Lambda表达式的类型，Lambda表达式可以看作是函数式接口的实现。

### 12.2 Lambda表达式
Lambda表达式是一种简洁的匿名函数表示方式，语法为(parameters) -> expression或(parameters) -> { statements; }。Lambda表达式可以捕获外部变量，但变量必须是final或实际final的。Lambda表达式提高了代码的简洁性和可读性。

### 12.3 方法引用
方法引用是Lambda表达式的简化形式，用::操作符表示。有四种方法引用类型：静态方法引用（Class::staticMethod）、实例方法引用（object::instanceMethod）、特定类型的任意对象的实例方法引用（Class::instanceMethod）、构造器引用（Class::new）。

### 12.4 Stream API
Stream API提供了函数式编程风格的集合操作方式。Stream是数据流的抽象，支持filter、map、reduce等操作。Stream操作分为中间操作和终端操作，中间操作返回新的Stream，终端操作产生结果。Stream支持并行处理，通过parallelStream()或parallel()方法启用。

## 13. 日期时间API

### 13.1 传统日期时间API
Java早期的日期时间API主要包括Date、Calendar、SimpleDateFormat等类。Date类表示特定时刻，但API设计有缺陷，大部分方法已被废弃。Calendar类提供了日期计算功能，但使用复杂且非线程安全。SimpleDateFormat用于日期格式化，但也不是线程安全的。

### 13.2 新日期时间API
Java 8引入了新的日期时间API，位于java.time包中。主要类包括LocalDate（日期）、LocalTime（时间）、LocalDateTime（日期时间）、ZonedDateTime（带时区的日期时间）、Instant（时间戳）等。新API设计良好，线程安全，API更加直观。

### 13.3 日期时间操作
新API提供了丰富的日期时间操作方法，如plus/minus系列方法用于加减操作，with系列方法用于设置特定字段，get系列方法用于获取字段值。Period和Duration分别表示基于日期和基于时间的时间量。DateTimeFormatter用于格式化和解析日期时间。

### 13.4 时区处理
ZoneId表示时区，ZonedDateTime表示带时区的日期时间。OffsetDateTime表示带偏移量的日期时间。时区处理在国际化应用中非常重要，新API提供了完善的时区支持。Instant表示UTC时间线上的一个点，用于记录事件的时间戳。

## 14. 设计模式

### 14.1 单例模式
单例模式确保一个类只有一个实例，并提供全局访问点。实现方式包括饿汉式（类加载时创建实例）、懒汉式（首次使用时创建实例）、双重检查锁定、静态内部类、枚举等。需要考虑线程安全、序列化、反射等问题。

### 14.2 工厂模式
工厂模式包括简单工厂、工厂方法、抽象工厂三种形式。简单工厂通过一个工厂类创建对象，工厂方法将对象创建延迟到子类，抽象工厂提供创建相关对象族的接口。工厂模式解耦了对象的创建和使用，提高了代码的灵活性。

### 14.3 观察者模式
观察者模式定义了对象间的一对多依赖关系，当一个对象状态改变时，所有依赖者都会收到通知。Java提供了Observable类和Observer接口支持观察者模式。现代实现通常使用事件驱动架构和回调机制。

### 14.4 装饰器模式
装饰器模式动态地给对象添加额外的职责，比继承更灵活。Java I/O流大量使用了装饰器模式，如BufferedInputStream装饰FileInputStream。装饰器模式遵循开放-封闭原则，对扩展开放，对修改封闭。

## 15. JVM相关

### 15.1 JVM内存结构
JVM内存分为方法区、堆、栈、PC寄存器、本地方法栈等区域。堆存储对象实例，分为年轻代和老年代。年轻代包括Eden区和两个Survivor区。方法区存储类信息、常量、静态变量等。栈存储方法调用的局部变量和操作数栈。

### 15.2 垃圾回收
垃圾回收自动管理内存，回收不再使用的对象。常用的垃圾回收算法包括标记-清除、复制算法、标记-整理。分代收集根据对象存活时间的不同采用不同的回收策略。常用的垃圾收集器包括Serial、Parallel、CMS、G1等。

### 15.3 类加载机制
类加载过程包括加载、验证、准备、解析、初始化五个阶段。类加载器负责加载类文件，采用双亲委派模型确保类加载的安全性。自定义类加载器可以实现动态加载、加密解密、热部署等功能。

### 15.4 JVM调优
JVM调优包括内存调优、垃圾回收调优、JIT编译优化等。常用参数包括-Xms（初始堆大小）、-Xmx（最大堆大小）、-XX:NewRatio（新生代与老年代比例）等。性能分析工具包括jstat、jmap、jhat、VisualVM等。

## 16. Java 8+新特性

### 16.1 Java 8新特性
Java 8引入了Lambda表达式、Stream API、函数式接口、方法引用、接口默认方法、新的日期时间API、Optional类、Base64编码等重要特性。这些特性极大地改善了Java的编程体验，使Java支持函数式编程。

### 16.2 Java 9新特性
Java 9引入了模块系统（Project Jigsaw）、JShell（交互式Java Shell）、改进的Stream API、私有接口方法、改进的Optional类、改进的@Deprecated注解等。模块系统是最重要的变化，提供了更好的封装和依赖管理。

### 16.3 Java 11新特性
Java 11是长期支持版本，引入了局部变量类型推断（var关键字在Lambda中的使用）、新的String方法、新的文件API、标准化HTTP Client API、废弃的Nashorn JavaScript引擎等特性。

### 16.4 后续版本特性
Java 12引入了Switch表达式的预览版。Java 13改进了Switch表达式，引入了文本块的预览版。Java 14正式引入了Switch表达式，改进了文本块，引入了Records的预览版。Java 15正式引入了文本块。Java 16正式引入了Records，Java 17是下一个LTS版本。

## 17. 性能优化

### 17.1 代码层面优化
优化字符串操作，使用StringBuilder而不是String连接。合理使用集合类，根据使用场景选择合适的数据结构。避免创建不必要的对象，重用对象。使用局部变量而不是成员变量。合理使用缓存，避免重复计算。

### 17.2 算法和数据结构优化
选择合适的算法，分析时间复杂度和空间复杂度。使用高效的数据结构，如HashMap的O(1)查找。避免嵌套循环，减少算法复杂度。使用分而治之、动态规划等优化策略。

### 17.3 JVM层面优化
合理设置JVM参数，如堆大小、垃圾收集器选择。监控GC日志，分析垃圾回收性能。使用对象池减少对象创建开销。合理使用JIT编译优化。

### 17.4 系统层面优化
使用连接池、线程池等池化技术。合理使用缓存，如Redis、Memcached。数据库优化，包括索引优化、SQL优化。使用异步编程提高并发性能。

## 18. 常用工具和框架

### 18.1 构建工具
Maven是Java项目的主流构建工具，使用XML配置文件管理依赖和构建过程。Gradle是基于Groovy的构建工具，配置更加灵活。Ant是早期的构建工具，现在使用较少。IDE集成了构建工具，简化了项目管理。

### 18.2 测试框架
JUnit是Java最流行的单元测试框架，JUnit 5提供了更强大的功能。TestNG是另一个测试框架，支持更复杂的测试配置。Mockito用于创建模拟对象，简化单元测试。集成测试、性能测试也有相应的工具支持。

### 18.3 日志框架
Java有多种日志框架，如JUL、Log4j、Logback、SLF4J等。SLF4J是日志门面，提供统一的日志API。Logback是Log4j的改进版，性能更好。合理配置日志级别和输出格式对于问题诊断很重要。

### 18.4 Spring框架
Spring是Java企业级开发的主流框架，提供依赖注入、AOP、事务管理等功能。Spring Boot简化了Spring应用的配置和部署。Spring Cloud提供了微服务开发的全套解决方案。Spring生态系统庞大，包含众多子项目。

## 19. 最佳实践

### 19.1 编码规范
遵循Java命名规范，类名使用大驼峰，方法名和变量名使用小驼峰，常量使用大写字母和下划线。合理使用注释，解释复杂的业务逻辑。保持方法简短，单一职责。使用有意义的变量名和方法名。

### 19.2 异常处理最佳实践
不要忽略异常，至少要记录日志。不要使用异常控制程序流程。优先使用标准异常。在合适的层级处理异常。提供有用的异常信息。及时关闭资源，使用try-with-resources语句。

### 19.3 并发编程最佳实践
优先使用线程安全的集合类。正确使用synchronized关键字。避免死锁，注意锁的获取顺序。使用线程池而不是直接创建线程。合理使用volatile关键字。理解happens-before关系。

### 19.4 性能最佳实践
避免过早优化，先保证正确性再考虑性能。使用性能分析工具定位瓶颈。合理使用缓存，但要注意缓存一致性。选择合适的数据结构和算法。避免内存泄漏，及时释放资源。

## 20. 企业级开发

### 20.1 数据访问层
使用ORM框架如Hibernate、MyBatis简化数据库操作。合理设计数据模型，使用适当的关系映射。实现数据访问层的抽象，使用DAO模式。考虑数据库事务的ACID特性。

### 20.2 业务逻辑层
保持业务逻辑的独立性，不依赖具体的技术实现。使用设计模式提高代码的可维护性。实现合适的异常处理策略。考虑业务流程的事务性。

### 20.3 表现层
分离视图和控制逻辑，使用MVC模式。合理处理用户输入，防止安全漏洞。实现合适的错误处理和用户提示。考虑用户体验和界面友好性。

### 20.4 系统架构
设计松耦合的系统架构，便于维护和扩展。合理划分系统层次，明确各层职责。考虑系统的可扩展性和可维护性。实现合适的监控和日志记录。