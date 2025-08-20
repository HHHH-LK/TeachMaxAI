package com.aiproject.smartcampus.test.ab;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * 模型/数据类集合（为简化文件数量，使用包可见类）
 * 注意：同包内其他类可直接使用这些类型
 */

interface RAGSystem {
    String getName();
    void indexDocuments(java.util.List<dev.langchain4j.data.document.Document> documents);
    RAGResponse retrieve(String query);
}

class RAGResponse {
    private String answer;
    private List<RetrievedDocument> retrievedDocuments;
    private long retrievalTimeMs;
    private long generationTimeMs;

    public RAGResponse(String answer, List<RetrievedDocument> retrievedDocuments) {
        this.answer = answer;
        this.retrievedDocuments = retrievedDocuments;
    }

    public String getAnswer() { return answer; }
    public List<RetrievedDocument> getRetrievedDocuments() { return retrievedDocuments; }
    public long getRetrievalTimeMs() { return retrievalTimeMs; }
    public void setRetrievalTimeMs(long retrievalTimeMs) { this.retrievalTimeMs = retrievalTimeMs; }
    public long getGenerationTimeMs() { return generationTimeMs; }
    public void setGenerationTimeMs(long generationTimeMs) { this.generationTimeMs = generationTimeMs; }
}

class RetrievedDocument {
    private String id;
    private String content;
    private double relevanceScore;
    private int rank;

    public RetrievedDocument(String id, String content, double relevanceScore, int rank) {
        this.id = id;
        this.content = content;
        this.relevanceScore = relevanceScore;
        this.rank = rank;
    }

    public String getId() { return id; }
    public String getContent() { return content; }
    public double getRelevanceScore() { return relevanceScore; }
    public int getRank() { return rank; }
}

class TestCase {
    private String query;
    private List<String> expectedDocIds;
    private String expectedAnswerPattern;

    public TestCase(String query, List<String> expectedDocIds, String expectedAnswerPattern) {
        this.query = query;
        this.expectedDocIds = expectedDocIds;
        this.expectedAnswerPattern = expectedAnswerPattern;
    }

    public String getQuery() { return query; }
    public List<String> getExpectedDocIds() { return expectedDocIds; }
    public String getExpectedAnswerPattern() { return expectedAnswerPattern; }
}

class SystemMetrics {
    private double avgRetrievalTime;
    private double avgGenerationTime;
    private double avgPrecision;
    private double avgRecall;
    private double avgF1Score;

    public void print() {
        System.out.printf("  平均检索时间: %.2f ms%n", avgRetrievalTime);
        System.out.printf("  平均生成时间: %.2f ms%n", avgGenerationTime);
        System.out.printf("  平均精确度: %.3f%n", avgPrecision);
        System.out.printf("  平均召回率: %.3f%n", avgRecall);
        System.out.printf("  平均F1分数: %.3f%n", avgF1Score);
    }

    public double getAvgRetrievalTime() { return avgRetrievalTime; }
    public void setAvgRetrievalTime(double avgRetrievalTime) { this.avgRetrievalTime = avgRetrievalTime; }
    public double getAvgGenerationTime() { return avgGenerationTime; }
    public void setAvgGenerationTime(double avgGenerationTime) { this.avgGenerationTime = avgGenerationTime; }
    public double getAvgPrecision() { return avgPrecision; }
    public void setAvgPrecision(double avgPrecision) { this.avgPrecision = avgPrecision; }
    public double getAvgRecall() { return avgRecall; }
    public void setAvgRecall(double avgRecall) { this.avgRecall = avgRecall; }
    public double getAvgF1Score() { return avgF1Score; }
    public void setAvgF1Score(double avgF1Score) { this.avgF1Score = avgF1Score; }
}

class TestCaseResult {
    private String query;
    private Map<String, RAGResponse> systemResponses = new HashMap<>();

    public TestCaseResult(String query) { this.query = query; }
    public String getQuery() { return query; }
    public Map<String, RAGResponse> getSystemResponses() { return systemResponses; }
    public void addSystemResponse(String systemName, RAGResponse response) {
        systemResponses.put(systemName, response);
    }
}

/**
 * 唯一 public，方便其它包序列化输出时不受限制
 */
public class ABTestResult {
    private Map<String, SystemMetrics> systemMetrics = new HashMap<>();
    private List<TestCaseResult> testCaseResults = new ArrayList<>();

    public void addSystemMetrics(String systemName, SystemMetrics metrics) {
        systemMetrics.put(systemName, metrics);
    }

    public void addTestCaseResult(TestCaseResult result) {
        testCaseResults.add(result);
    }

    public void printDetailedReport() {
        System.out.println("\n=== Java程序设计RAG系统A/B测试详细报告 ===");
        for (Map.Entry<String, SystemMetrics> entry : systemMetrics.entrySet()) {
            System.out.println("\n系统: " + entry.getKey());
            entry.getValue().print();
        }
    }

    public void exportToJSON(String filename) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            FileWriter writer = new FileWriter(filename);
            mapper.writerWithDefaultPrettyPrinter().writeValue(writer, this);
            writer.close();
            System.out.println("Java程序设计测试结果已导出到: " + filename);
        } catch (IOException e) {
            System.err.println("导出失败: " + e.getMessage());
        }
    }

    public Map<String, SystemMetrics> getSystemMetrics() { return systemMetrics; }
    public List<TestCaseResult> getTestCaseResults() { return testCaseResults; }
}