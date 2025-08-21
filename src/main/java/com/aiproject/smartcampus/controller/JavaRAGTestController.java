package com.aiproject.smartcampus.controller;


import com.aiproject.smartcampus.test.ab.JavaPerformanceTest;
import com.aiproject.smartcampus.test.ab.JavaRAGABTestIntegration;
import com.aiproject.smartcampus.test.ab.JavaTestDataGenerator;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.aiproject.smartcampus.model.handler.impl.SeptIntentRagHandler;

import java.util.*;

@RestController
@RequestMapping("/test")
public class JavaRAGTestController {

    @Autowired
    private SeptIntentRagHandler handler;
    @Autowired
    private ContentRetriever contentRetriever; // 使用 @Primary 的那份
    @Autowired
    private ChatLanguageModel chatModel;

    @Autowired
    private EmbeddingStore<TextSegment> store;
    @Autowired
    private EmbeddingModel embeddingModel;

    /**
     * 运行 Java 程序设计 RAG 的 A/B 测试
     */
    @GetMapping("/java-rag-ab")
    public String runAb() {
        // 1) 生成测试语料
        List<Document> docs = JavaTestDataGenerator.generateJavaDocuments();

        // 2) 切分 + 向量化 + 入库（与 Handler 共用同一 store）
        var splitter = DocumentSplitters.recursive(400, 80);
        var ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(splitter)
                .embeddingModel(embeddingModel)
                .embeddingStore(store)
                .build();
        ingestor.ingest(docs);

        // 3) 启动 A/B 测试（传统 vs 增强型）
        JavaRAGABTestIntegration.runJavaRAGABTest(handler, contentRetriever, chatModel);
        return "Java 程序设计 RAG A/B 测试已完成，请查看控制台输出和结果文件";
    }

    @GetMapping("/RAG/test")
    public String runRAG(@RequestParam String userQuery) {

        return handler.executeDirectTask(userQuery);

    }

    /**
     * 运行 Java 程序设计专项性能测试
     */
    @GetMapping("/java-performance")
    public Map<String, Object> runPerformance() {
        // 确保已入库（可选重复执行无伤大雅）
        List<Document> docs = JavaTestDataGenerator.generateJavaDocuments();
        var splitter = DocumentSplitters.recursive(400, 80);
        var ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(splitter)
                .embeddingModel(embeddingModel)
                .embeddingStore(store)
                .build();
        ingestor.ingest(docs);

        // 性能测试
        JavaPerformanceTest perfTest =
                new com.aiproject.smartcampus.test.ab.JavaPerformanceTest(handler, contentRetriever);
        perfTest.testConcurrentPerformance();
        perfTest.testJavaRetrievalQuality();

        Map<String, Object> result = new HashMap<>();
        result.put("status", "completed");
        result.put("testType", "Java程序设计RAG性能测试");
        result.put("timestamp", new Date());
        return result;
    }
}