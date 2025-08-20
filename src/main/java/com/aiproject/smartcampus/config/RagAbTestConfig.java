package com.aiproject.smartcampus.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("abtest")
public class RagAbTestConfig {

    /**
     * 测试使用的内存向量库（与 Handler 共用）
     */
    @Bean
    public EmbeddingStore<TextSegment> testEmbeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    /**
     * 将 ContentRetriever 设置为 @Primary，确保 Handler 与 A/B 测试用的是同一份
     */
    @Bean
    @Primary
    public ContentRetriever testContentRetriever(EmbeddingStore<TextSegment> store,
                                                 EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .embeddingModel(embeddingModel)
                .maxResults(30)
                .minScore(0.20)
                .build();
    }
}