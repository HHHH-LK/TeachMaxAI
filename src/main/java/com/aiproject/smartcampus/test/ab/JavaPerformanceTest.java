package com.aiproject.smartcampus.test.ab;

import com.aiproject.smartcampus.model.handler.impl.SeptIntentRagHandler;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
public class JavaPerformanceTest {

    private final SeptIntentRagHandler handler;
    private final ContentRetriever retriever;

    public JavaPerformanceTest(SeptIntentRagHandler handler, ContentRetriever retriever) {
        this.handler = handler;
        this.retriever = retriever;
    }

    public void testConcurrentPerformance() {
        List<String> javaQueries = Arrays.asList(
                "Java基础数据类型有哪些",
                "面向对象三大特性",
                "异常处理机制",
                "集合框架使用方法",
                "多线程编程要点",
                "IO流操作技巧",
                "设计模式应用场景",
                "JVM内存管理原理",
                "泛型编程规范",
                "反射机制使用"
        );

        long startTime = System.currentTimeMillis();

        List<CompletableFuture<String>> futures = javaQueries.stream()
                .map(query -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return handler.executeBusinessLogic(query, new ArrayList<>());
                    } catch (Exception e) {
                        return "Error: " + e.getMessage();
                    }
                }))
                .toList();

        List<String> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        log.info("RAG检索结果为: {}", results);

        long totalTime = System.currentTimeMillis() - startTime;

        System.out.println("=== Java程序设计并发性能测试结果 ===");
        System.out.println("Java查询数量: " + javaQueries.size());
        System.out.println("总耗时: " + totalTime + "ms");
        System.out.println("平均每查询: " + (totalTime / javaQueries.size()) + "ms");
        System.out.println("成功率: " + results.stream()
                .filter(r -> r != null && !r.startsWith("Error"))
                .count() + "/" + results.size());
    }

    public void testJavaRetrievalQuality() {
        Map<String, List<String>> queryToExpectedDocs = new HashMap<>();
        queryToExpectedDocs.put("Java数据类型", Arrays.asList("java_basic_001"));
        queryToExpectedDocs.put("面向对象特性", Arrays.asList("java_oop_001"));
        queryToExpectedDocs.put("异常处理", Arrays.asList("java_exception_001"));
        queryToExpectedDocs.put("集合框架", Arrays.asList("java_collection_001"));
        queryToExpectedDocs.put("多线程编程", Arrays.asList("java_thread_001"));
        queryToExpectedDocs.put("IO操作", Arrays.asList("java_io_001"));
        queryToExpectedDocs.put("设计模式", Arrays.asList("java_pattern_001"));
        queryToExpectedDocs.put("内存管理", Arrays.asList("java_memory_001"));

        double totalPrecision = 0;
        int testCount = 0;

        System.out.println("=== Java知识检索质量测试 ===");
        for (Map.Entry<String, List<String>> entry : queryToExpectedDocs.entrySet()) {
            String query = entry.getKey();
            List<String> expectedDocs = entry.getValue();

            try {
                List<Content> retrieved = retriever.retrieve(Query.from(query));

                long matches = retrieved.stream()
                        .filter(content -> expectedDocs.stream()
                                .anyMatch(docId -> {
                                    if (content.textSegment() == null || content.textSegment().metadata() == null)
                                        return false;
                                    var md = content.textSegment().metadata();
                                    return md.containsKey("id") && docId.equals(md.getString("id"));
                                }))
                        .count();

                double precision = retrieved.isEmpty() ? 0 : (double) matches / retrieved.size();
                totalPrecision += precision;
                testCount++;

                System.out.printf("Java查询: %s, 精确度: %.3f, 检索数量: %d%n",
                        query, precision, retrieved.size());

            } catch (Exception e) {
                System.err.println("Java查询失败: " + query + " - " + e.getMessage());
            }
        }

        if (testCount > 0) {
            System.out.printf("Java知识库平均精确度: %.3f%n", totalPrecision / testCount);
        }
    }
}