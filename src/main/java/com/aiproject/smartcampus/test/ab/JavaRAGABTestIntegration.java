package com.aiproject.smartcampus.test.ab;

import com.aiproject.smartcampus.model.handler.impl.SeptIntentRagHandler;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;

import java.nio.file.Paths;
import java.util.List;

public class JavaRAGABTestIntegration {

    /**
     * 运行 Java 程序设计 RAG 的 A/B 测试
     * - 使用 TraditionalRAG 作为基线
     * - 使用 RealRAGHandlerAdapter（你的 SeptIntentRagHandler + ContentRetriever）作为增强型
     * - 打印详细报告，并导出到工程根目录 java_rag_ab_result.json
     */
    public static ABTestResult runJavaRAGABTest(SeptIntentRagHandler handler,
                                                ContentRetriever contentRetriever,
                                                ChatLanguageModel chatModel) {
        // 构造两套系统
        RAGSystem baseline = new TraditionalRAG();
        RAGSystem enhanced = new RealRAGHandlerAdapter(
                handler, contentRetriever, chatModel, "增强型Java教学RAG系统");

        // 测试数据
        List<Document> docs = JavaTestDataGenerator.generateJavaDocuments();
        List<TestCase> testCases = JavaTestDataGenerator.generateJavaTestCases();

        // 运行 A/B 测试
        ABTestRunner runner = new ABTestRunner(baseline, enhanced);
        runner.initialize(docs, testCases);
        ABTestResult result = runner.runTest();

        // 输出报告并导出 JSON
        result.printDetailedReport();
        String filename = Paths.get(System.getProperty("user.dir"), "java_rag_ab_result.json").toString();
        result.exportToJSON(filename);

        return result;
    }
}