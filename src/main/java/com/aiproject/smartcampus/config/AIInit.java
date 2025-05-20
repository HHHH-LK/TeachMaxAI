package com.aiproject.smartcampus.config;

import com.aiproject.smartcampus.pojo.bo.ToolList;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
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
    private final ToolList toolList;

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
                .maxResults(10)
                .minScore(0.7)
                .build();

        return embeddingStoreContentRetriever;
    }

    //工具声明
    @Bean
    public ToolSpecification weatherTool() {
        ToolSpecification weathertool = ToolSpecification.builder()
                .name("WeatherTool")
                .description("查询天气怎么样")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("city", "城市名字")
                        .required("city")
                        .build())
                .build();
        //将声明的tool存入list种
        List<ToolSpecification> tools = toolList.getTools();
        //判断是否已经存在
        boolean exists = tools.stream().anyMatch(tool -> "WeatherTool".equals(tool.name()));
        if (!exists) {
            tools.add(weathertool);
        }

        return weathertool;
    }

}
