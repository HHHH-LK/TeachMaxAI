package com.aiproject.smartcampus.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @program: SmartCampus
 * @description: 文件处理
 * @author: lk
 * @create: 2025-05-17 16:52
 **/

@Component
@RequiredArgsConstructor
public class FileloadFunction {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore embeddingStore;
    private final ChatLanguageModel chatLanguageModel;
    private final LocalTokenizerFuncation tokenizer;

    public void documentsloade(){
        //加载文件(通过自定义解析器来解析文件)
        List<Document> documents = FileSystemDocumentLoader.loadDocuments("documents", new ApacheTikaDocumentParser());
        //进行切分处理
        DocumentByParagraphSplitter documentByParagraphSplitter = new DocumentByParagraphSplitter(600, 50, tokenizer);
        List<TextSegment> textSegments = documentByParagraphSplitter.splitAll(documents);
        //对切分的数据再进行切分
        for(TextSegment textSegment : textSegments) {
            //转换成document
            Document document = Document.from(textSegment.text());
            ChatResponse chatResponse = chatLanguageModel
                    .chat(SystemMessage.systemMessage("将该内容进行概括成只有 100 token的文本"), UserMessage.userMessage(document.text()));
            //存入总结
            String content = chatResponse.aiMessage().text();
            //对 document 进行二次拆分
            DocumentBySentenceSplitter documentBySentenceSplitter = new DocumentBySentenceSplitter(100, 50, tokenizer);
            List<TextSegment> split = documentBySentenceSplitter.split(document);
            //存储到本地
            List<Embedding> embeddingList = embeddingModel.embedAll(split).content();
            embeddingStore.addAll(embeddingList, split);
            //对总结内容进行存储
            Embedding embedding = embeddingModel.embed(content).content();
            embeddingStore.add(embedding, TextSegment.from(content));
    }

}


}
