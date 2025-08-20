package com.aiproject.smartcampus.test.ab;

import com.aiproject.smartcampus.model.handler.impl.SeptIntentRagHandler;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * 将 SeptIntentRagHandler 适配到 A/B 测试框架
 */
class RealRAGHandlerAdapter implements RAGSystem {

    private final SeptIntentRagHandler handler;
    private final ContentRetriever contentRetriever;
    private final ChatLanguageModel chatModel;
    private final String systemName;
    private List<dev.langchain4j.data.document.Document> indexedDocuments = new ArrayList<>();

    public RealRAGHandlerAdapter(SeptIntentRagHandler handler,
                                 ContentRetriever contentRetriever,
                                 ChatLanguageModel chatModel,
                                 String systemName) {
        this.handler = handler;
        this.contentRetriever = contentRetriever;
        this.chatModel = chatModel;
        this.systemName = systemName;
    }

    @Override
    public String getName() {
        return systemName;
    }

    @Override
    public void indexDocuments(List<dev.langchain4j.data.document.Document> documents) {
        this.indexedDocuments = new ArrayList<>(documents);
        System.out.println("已索引 " + documents.size() + " 个Java程序设计文档到 " + systemName);
    }

    @Override
    public RAGResponse retrieve(String query) {
        long startTime = System.currentTimeMillis();

        try {
            String handlerResult = handler.executeBusinessLogic(query, new ArrayList<>());

            List<Content> retrievedContents = contentRetriever.retrieve(Query.from(query));
            List<RetrievedDocument> retrievedDocs = convertToRetrievedDocuments(retrievedContents);

            long retrievalTime = System.currentTimeMillis() - startTime;

            RAGResponse response = new RAGResponse(handlerResult, retrievedDocs);
            response.setRetrievalTimeMs(retrievalTime);
            response.setGenerationTimeMs(0);
            return response;
        } catch (Exception e) {
            System.err.println("Java程序设计Handler执行失败: " + e.getMessage());
            RAGResponse errorResponse = new RAGResponse(
                    "Java程序设计处理失败: " + e.getMessage(),
                    new ArrayList<>()
            );
            errorResponse.setRetrievalTimeMs(System.currentTimeMillis() - startTime);
            return errorResponse;
        }
    }

    private List<RetrievedDocument> convertToRetrievedDocuments(List<Content> contents) {
        List<RetrievedDocument> result = new ArrayList<>();

        for (int i = 0; i < contents.size(); i++) {
            Content content = contents.get(i);

            String docId = "java_retrieved_" + i;
            if (content.textSegment() != null && content.textSegment().metadata() != null) {
                Metadata metadata = content.textSegment().metadata();
                if (metadata.containsKey("id")) {
                    docId = metadata.getString("id");
                }
            }

            String text = content.textSegment() != null ?
                    content.textSegment().text() : content.toString();

            double relevanceScore = Math.max(0, 1.0 - i * 0.1);

            result.add(new RetrievedDocument(docId, text, relevanceScore, i + 1));
        }

        return result;
    }
}