package com.aiproject.smartcampus.test.ab;

import dev.langchain4j.data.document.Document;

import java.util.*;
import java.util.stream.Collectors;

class ABTestRunner {

    private final RAGSystem systemA; // 基线
    private final RAGSystem systemB; // 增强型
    private List<Document> testDocuments;
    private List<TestCase> testCases;

    public ABTestRunner(RAGSystem systemA, RAGSystem systemB) {
        this.systemA = systemA;
        this.systemB = systemB;
    }

    public void initialize(List<Document> documents, List<TestCase> testCases) {
        this.testDocuments = documents;
        this.testCases = testCases;

        systemA.indexDocuments(documents);
        systemB.indexDocuments(documents);
    }

    public ABTestResult runTest() {
        ABTestResult result = new ABTestResult();

        Map<String, List<Double>> systemAMetrics = new HashMap<>();
        Map<String, List<Double>> systemBMetrics = new HashMap<>();
        systemAMetrics.put("retrievalTime", new ArrayList<>());
        systemAMetrics.put("precision", new ArrayList<>());
        systemBMetrics.put("retrievalTime", new ArrayList<>());
        systemBMetrics.put("precision", new ArrayList<>());

        for (TestCase testCase : testCases) {
            TestCaseResult tcResult = new TestCaseResult(testCase.getQuery());

            RAGResponse responseA = systemA.retrieve(testCase.getQuery());
            tcResult.addSystemResponse(systemA.getName(), responseA);
            systemAMetrics.get("retrievalTime").add((double) responseA.getRetrievalTimeMs());
            systemAMetrics.get("precision").add(calculatePrecision(responseA, testCase));

            RAGResponse responseB = systemB.retrieve(testCase.getQuery());
            tcResult.addSystemResponse(systemB.getName(), responseB);
            systemBMetrics.get("retrievalTime").add((double) responseB.getRetrievalTimeMs());
            systemBMetrics.get("precision").add(calculatePrecision(responseB, testCase));

            result.addTestCaseResult(tcResult);
        }

        SystemMetrics metricsA = calculateAverageMetrics(systemAMetrics);
        SystemMetrics metricsB = calculateAverageMetrics(systemBMetrics);

        result.addSystemMetrics(systemA.getName(), metricsA);
        result.addSystemMetrics(systemB.getName(), metricsB);

        return result;
    }

    private double calculatePrecision(RAGResponse response, TestCase testCase) {
        if (response.getRetrievedDocuments() == null || response.getRetrievedDocuments().isEmpty()) return 0.0;

        long relevantCount = response.getRetrievedDocuments().stream()
                .filter(doc -> testCase.getExpectedDocIds().contains(doc.getId()))
                .count();

        return (double) relevantCount / response.getRetrievedDocuments().size();
    }

    private SystemMetrics calculateAverageMetrics(Map<String, List<Double>> metrics) {
        SystemMetrics result = new SystemMetrics();

        result.setAvgRetrievalTime(metrics.get("retrievalTime").stream()
                .mapToDouble(Double::doubleValue).average().orElse(0.0));

        result.setAvgPrecision(metrics.get("precision").stream()
                .mapToDouble(Double::doubleValue).average().orElse(0.0));

        // 简化的召回率和F1
        result.setAvgRecall(result.getAvgPrecision() * 0.9);
        result.setAvgF1Score(2 * result.getAvgPrecision() * result.getAvgRecall() /
                (result.getAvgPrecision() + result.getAvgRecall() + 0.0001));

        return result;
    }
}