package com.aiproject.smartcampus.test.ab;

import dev.langchain4j.data.document.Document;

import java.util.ArrayList;
import java.util.List;

class TraditionalRAG implements RAGSystem {

    private List<Document> documents = new ArrayList<>();

    @Override
    public String getName() {
        return "传统Java教学RAG系统（基线）";
    }

    @Override
    public void indexDocuments(List<Document> documents) {
        this.documents = new ArrayList<>(documents);
        System.out.println("传统Java教学RAG系统已索引 " + documents.size() + " 个Java知识文档");
    }

    @Override
    public RAGResponse retrieve(String query) {
        long startTime = System.currentTimeMillis();

        List<RetrievedDocument> retrieved = new ArrayList<>();
        int rank = 1;
        for (Document doc : documents) {
            if (doc.text().toLowerCase().contains(query.toLowerCase())) {
                retrieved.add(new RetrievedDocument(
                        doc.metadata().getString("id"),
                        doc.text(),
                        0.8 - rank * 0.1,
                        rank++
                ));
                if (retrieved.size() >= 3) break;
            }
        }

        String answer = "基于Java程序设计知识库检索，答案是：" +
                (retrieved.isEmpty()
                        ? "未找到相关Java知识"
                        : retrieved.getFirst().getContent().substring(0, Math.min(150, retrieved.get(0).getContent().length())));

        RAGResponse response = new RAGResponse(answer, retrieved);
        response.setRetrievalTimeMs(System.currentTimeMillis() - startTime);
        return response;
    }
}