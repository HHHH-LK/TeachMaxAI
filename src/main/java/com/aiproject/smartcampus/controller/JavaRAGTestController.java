package com.aiproject.smartcampus.controller;


import com.aiproject.smartcampus.model.rag.FileloadFunction;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.aiproject.smartcampus.model.handler.impl.SeptIntentRagHandler;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@Slf4j
@RequestMapping("/test")
public class JavaRAGTestController {

    @Autowired
    private SeptIntentRagHandler handler;
    @Autowired
    private ContentRetriever contentRetriever; // 使用 @Primary 的那份
    @Autowired
    private ChatLanguageModel chatModel;
    @Autowired
    private FileloadFunction fileloadFunction;
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

    /**
     * 导入知识库资料
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2
    );

    @GetMapping("/knowledge/database")
    public void loadKnowledgeDatabase(String url) {
        if (url == null || url.isEmpty()) {
            return;
        }

        // 搜索所有文件
        List<File> allFiles = searchAllFiles(url);
        log.info("开始知识库文件导入，一共【{}】个数据文件", allFiles.size());

        AtomicInteger i = new AtomicInteger();
        // 提交并发任务
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (File file : allFiles) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    i.getAndIncrement();
                    fileloadFunction.processDocumentDynamically(file);
                    log.info("第【{}】文件 {} 导入完毕", i, file.getName());
                } catch (Exception e) {
                    log.error("文件 {} 导入失败: {}", file.getName(), e.getMessage(), e);
                }
            }, executorService);
            futures.add(future);
        }

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("导入知识库文件成功，一共处理了【{}】条数据", allFiles.size());
    }

    /**
     * 从指定的父文件夹中搜索所有文件（包括子文件夹中的文件）
     *
     * @param parentFolderPath 父文件夹路径
     * @return 包含所有文件的List集合
     */
    public static List<File> searchAllFiles(String parentFolderPath) {
        List<File> fileList = new ArrayList<>();
        File parentFolder = new File(parentFolderPath);

        // 检查父文件夹是否存在且是一个目录
        if (!parentFolder.exists() || !parentFolder.isDirectory()) {
            System.out.println("错误：指定的路径不存在或不是一个文件夹 - " + parentFolderPath);
            return fileList;
        }

        // 递归搜索文件
        searchFilesRecursively(parentFolder, fileList);
        return fileList;
    }

    /**
     * 递归搜索文件夹中的所有文件
     *
     * @param folder   要搜索的文件夹
     * @param fileList 存储找到的文件的集合
     */
    private static void searchFilesRecursively(File folder, List<File> fileList) {
        // 获取文件夹中的所有文件和子文件夹
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    // 如果是文件，添加到集合中
                    fileList.add(file);
                } else if (file.isDirectory()) {
                    // 如果是文件夹，递归搜索
                    searchFilesRecursively(file, fileList);
                }
            }
        } else {
            // 处理无法访问的文件夹（如权限问题）
            System.out.println("警告：无法访问文件夹 - " + folder.getAbsolutePath());
        }
    }

}