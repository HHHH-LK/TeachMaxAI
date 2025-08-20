package com.aiproject.smartcampus.test.ab;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;

import java.util.*;

public class JavaTestDataGenerator {

    public static List<Document> generateJavaDocuments() {
        List<Document> documents = new ArrayList<>();

        documents.add(Document.from(
                "Java基础数据类型包括8种：byte、short、int、long、float、double、boolean、char。其中整型默认是int，浮点型默认是double。变量声明格式为：数据类型 变量名 = 初始值;",
                Metadata.from(Map.of(
                        "id", "java_basic_001",
                        "category", "基础语法",
                        "topic", "数据类型",
                        "difficulty", "初级",
                        "source", "Java程序设计教程"
                ))
        ));

        documents.add(Document.from(
                "Java面向对象编程三大特性：封装、继承、多态。封装通过private关键字实现信息隐藏，继承使用extends关键字实现代码复用，多态通过方法重写(Override)和接口实现。",
                Metadata.from(Map.of(
                        "id", "java_oop_001",
                        "category", "面向对象",
                        "topic", "OOP特性",
                        "difficulty", "中级",
                        "source", "Java程序设计教程"
                ))
        ));

        documents.add(Document.from(
                "Java异常处理机制使用try-catch-finally语句块。checked异常(如IOException)必须处理，unchecked异常(如RuntimeException)可选择处理。常见异常包括NullPointerException、ArrayIndexOutOfBoundsException、ClassCastException等。",
                Metadata.from(Map.of(
                        "id", "java_exception_001",
                        "category", "异常处理",
                        "topic", "异常机制",
                        "difficulty", "中级",
                        "source", "Java程序设计教程"
                ))
        ));

        documents.add(Document.from(
                "Java集合框架主要包括List、Set、Map三大接口。ArrayList适合随机访问和遍历，LinkedList适合频繁插入删除。HashMap基于哈希表实现快速查找，TreeMap基于红黑树实现有序存储。Vector是线程安全的动态数组。",
                Metadata.from(Map.of(
                        "id", "java_collection_001",
                        "category", "集合框架",
                        "topic", "集合类型",
                        "difficulty", "中级",
                        "source", "Java程序设计教程"
                ))
        ));

        documents.add(Document.from(
                "Java多线程编程可通过继承Thread类或实现Runnable接口创建线程。线程同步使用synchronized关键字或Lock接口防止数据竞争。ExecutorService线程池可以高效管理多个线程。线程状态包括NEW、RUNNABLE、BLOCKED、WAITING、TERMINATED。",
                Metadata.from(Map.of(
                        "id", "java_thread_001",
                        "category", "多线程",
                        "topic", "线程编程",
                        "difficulty", "高级",
                        "source", "Java程序设计教程"
                ))
        ));

        documents.add(Document.from(
                "Java IO操作分为字节流和字符流。InputStream/OutputStream处理字节数据，Reader/Writer处理字符数据。BufferedReader提高读取效率，FileWriter简化文件写入。NIO提供了更高效的非阻塞IO操作，支持Channel和Buffer。",
                Metadata.from(Map.of(
                        "id", "java_io_001",
                        "category", "IO操作",
                        "topic", "文件操作",
                        "difficulty", "中级",
                        "source", "Java程序设计教程"
                ))
        ));

        documents.add(Document.from(
                "常用Java设计模式包括单例模式、工厂模式、观察者模式、装饰器模式等。单例模式确保类只有一个实例，工厂模式用于创建对象而不暴露创建逻辑，观察者模式实现一对多的依赖关系，装饰器模式动态添加对象功能。",
                Metadata.from(Map.of(
                        "id", "java_pattern_001",
                        "category", "设计模式",
                        "topic", "常用模式",
                        "difficulty", "高级",
                        "source", "Java程序设计教程"
                ))
        ));

        documents.add(Document.from(
                "Java内存管理由JVM自动完成。堆内存存储对象实例和数组，栈内存存储局部变量和方法调用信息。方法区存储类信息和常量池。垃圾回收器(GC)自动回收不再引用的对象内存，常见GC算法包括标记-清除、复制算法、标记-整理。",
                Metadata.from(Map.of(
                        "id", "java_memory_001",
                        "category", "内存管理",
                        "topic", "JVM内存",
                        "difficulty", "高级",
                        "source", "Java程序设计教程"
                ))
        ));

        documents.add(Document.from(
                "Java泛型提供编译时类型安全检查。泛型类使用<T>声明类型参数，泛型方法在返回类型前声明类型参数。通配符?表示未知类型，? extends T表示上界，? super T表示下界。泛型擦除机制在运行时移除类型信息。",
                Metadata.from(Map.of(
                        "id", "java_generic_001",
                        "category", "泛型编程",
                        "topic", "类型安全",
                        "difficulty", "高级",
                        "source", "Java程序设计教程"
                ))
        ));

        documents.add(Document.from(
                "Java反射机制允许程序在运行时检查和操作类、方法、字段等。Class.forName()获取类对象，getMethod()获取方法，getField()获取字段。反射常用于框架开发、动态代理、注解处理等场景，但会影响性能和安全性。",
                Metadata.from(Map.of(
                        "id", "java_reflection_001",
                        "category", "反射机制",
                        "topic", "动态编程",
                        "difficulty", "高级",
                        "source", "Java程序设计教程"
                ))
        ));

        return documents;
    }

    public static List<TestCase> generateJavaTestCases() {
        List<TestCase> testCases = new ArrayList<>();

        testCases.add(new TestCase(
                "Java有哪些基础数据类型？默认类型是什么？",
                Arrays.asList("java_basic_001"),
                "byte.*short.*int.*long.*float.*double.*boolean.*char.*整型默认.*int.*浮点型默认.*double"
        ));
        testCases.add(new TestCase("如何在Java中声明变量？",
                Arrays.asList("java_basic_001"), "数据类型.*变量名.*初始值"));

        testCases.add(new TestCase(
                "Java面向对象编程的三大特性是什么？",
                Arrays.asList("java_oop_001"),
                "封装.*继承.*多态"
        ));
        testCases.add(new TestCase(
                "Java中如何实现继承和多态？",
                Arrays.asList("java_oop_001"),
                "extends.*方法重写.*Override.*接口实现"
        ));
        testCases.add(new TestCase(
                "什么是封装？如何实现？",
                Arrays.asList("java_oop_001"),
                "封装.*private.*信息隐藏"
        ));

        testCases.add(new TestCase(
                "Java异常处理使用什么语句？常见异常有哪些？",
                Arrays.asList("java_exception_001"),
                "try-catch-finally.*NullPointerException.*ArrayIndexOutOfBoundsException"
        ));
        testCases.add(new TestCase(
                "checked异常和unchecked异常有什么区别？",
                Arrays.asList("java_exception_001"),
                "checked.*必须处理.*unchecked.*可选择处理.*IOException.*RuntimeException"
        ));

        testCases.add(new TestCase(
                "ArrayList和LinkedList有什么区别？各自适用于什么场景？",
                Arrays.asList("java_collection_001"),
                "ArrayList.*随机访问.*遍历.*LinkedList.*插入删除"
        ));
        testCases.add(new TestCase(
                "HashMap和TreeMap的实现原理和特点是什么？",
                Arrays.asList("java_collection_001"),
                "HashMap.*哈希表.*快速查找.*TreeMap.*红黑树.*有序存储"
        ));
        testCases.add(new TestCase(
                "Java集合框架的主要接口有哪些？",
                Arrays.asList("java_collection_001"),
                "List.*Set.*Map.*三大接口"
        ));

        testCases.add(new TestCase(
                "Java中创建线程有哪些方式？",
                Arrays.asList("java_thread_001"),
                "Thread类.*Runnable接口"
        ));
        testCases.add(new TestCase(
                "如何实现线程同步？线程池如何使用？",
                Arrays.asList("java_thread_001"),
                "synchronized.*Lock接口.*ExecutorService.*线程池"
        ));
        testCases.add(new TestCase(
                "Java线程有哪些状态？",
                Arrays.asList("java_thread_001"),
                "NEW.*RUNNABLE.*BLOCKED.*WAITING.*TERMINATED"
        ));

        testCases.add(new TestCase(
                "Java IO操作分为哪几种流？各有什么特点？",
                Arrays.asList("java_io_001"),
                "字节流.*字符流.*InputStream.*OutputStream.*Reader.*Writer"
        ));
        testCases.add(new TestCase(
                "如何提高Java文件读写效率？",
                Arrays.asList("java_io_001"),
                "BufferedReader.*FileWriter.*NIO.*Channel.*Buffer"
        ));

        testCases.add(new TestCase(
                "常用的Java设计模式有哪些？各自的作用是什么？",
                Arrays.asList("java_pattern_001"),
                "单例模式.*工厂模式.*观察者模式.*装饰器模式"
        ));
        testCases.add(new TestCase(
                "单例模式和工厂模式分别解决什么问题？",
                Arrays.asList("java_pattern_001"),
                "单例模式.*一个实例.*工厂模式.*创建对象.*创建逻辑"
        ));

        testCases.add(new TestCase(
                "Java内存是如何管理的？JVM内存结构包括哪些部分？",
                Arrays.asList("java_memory_001"),
                "JVM.*堆内存.*栈内存.*方法区.*垃圾回收"
        ));
        testCases.add(new TestCase(
                "常见的垃圾回收算法有哪些？",
                Arrays.asList("java_memory_001"),
                "标记-清除.*复制算法.*标记-整理"
        ));

        testCases.add(new TestCase(
                "Java泛型有什么作用？如何使用通配符？",
                Arrays.asList("java_generic_001"),
                "编译时.*类型安全.*通配符.*extends.*super"
        ));

        testCases.add(new TestCase(
                "Java反射机制有什么用途？常用方法有哪些？",
                Arrays.asList("java_reflection_001"),
                "运行时.*检查.*操作.*Class.forName.*getMethod.*getField"
        ));

        testCases.add(new TestCase(
                "Java中如何处理文件操作异常？",
                Arrays.asList("java_io_001", "java_exception_001"),
                "IO操作.*try-catch.*异常处理.*IOException"
        ));
        testCases.add(new TestCase(
                "使用多线程时需要注意什么？如何避免线程安全问题？",
                Arrays.asList("java_thread_001", "java_oop_001"),
                "线程同步.*synchronized.*多线程.*安全.*数据竞争"
        ));
        testCases.add(new TestCase(
                "泛型和集合框架如何结合使用？",
                Arrays.asList("java_generic_001", "java_collection_001"),
                "泛型.*类型安全.*List.*Set.*Map"
        ));

        testCases.add(new TestCase(
                "什么时候使用ArrayList，什么时候使用LinkedList？需要考虑哪些因素？",
                Arrays.asList("java_collection_001"),
                "ArrayList.*随机访问.*LinkedList.*频繁插入删除.*性能"
        ));
        testCases.add(new TestCase(
                "设计一个Java类时应该考虑哪些面向对象原则？",
                Arrays.asList("java_oop_001", "java_pattern_001"),
                "封装.*继承.*多态.*设计模式"
        ));

        return testCases;
    }
}