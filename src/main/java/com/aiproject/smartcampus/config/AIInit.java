package com.aiproject.smartcampus.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;

import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeServerlessIndexConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * @program: SmartCampus
 * @description: AI相关初始化
 * @author: lk
 * @create: 2025-05-17 16:05
 **/
@Configuration
@RequiredArgsConstructor
public class AIInit {

    private final EmbeddingModel embeddingModel;

    //向量数据库的配置
    @Bean
    public EmbeddingStore PineconeEmbeddingStore() {

        EmbeddingStore<TextSegment> embeddingStore = PineconeEmbeddingStore.builder()
                //读取系统变量中的数据
                .apiKey(System.getenv("PINECONE_API_KEY"))
                .index("index-v3")
                .nameSpace("SmartCampus-namespace")
                .createIndex(PineconeServerlessIndexConfig.builder()
                        .cloud("AWS")
                        .region("us-east-1")
                        .dimension(embeddingModel.dimension()) //配置向量数据库的维度
                        .build())
                .build();

        return embeddingStore;
    }

    //构建检索
    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore embeddingStore) {

        EmbeddingStoreContentRetriever embeddingStoreContentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(20)
                .minScore(0.8)
                .build();

        return embeddingStoreContentRetriever;
    }





}
