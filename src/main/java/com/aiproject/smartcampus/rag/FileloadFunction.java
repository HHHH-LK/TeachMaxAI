package com.aiproject.smartcampus.rag;

import com.aiproject.smartcampus.commons.utils.JsonUtils;
import com.aiproject.smartcampus.pojo.po.Course;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @program: SmartCampus
 * @description: 文件处理
 * @author: lk
 * @create: 2025-05-17 16:52
 **/

@Slf4j
@Component
@RequiredArgsConstructor
public class FileloadFunction {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore embeddingStore;
    private final ChatLanguageModel chatLanguageModel;
    private final LocalTokenizerFuncation tokenizer;
    private final   String extractionPrompt = """
                系统提示（System / Assistant 身份设定）：
                                 你是⼀名高效且严谨的教务助理 AI，专⻔用于解析高校课程表并输出结构化数据。你的目标是：
                                 1. 识别 Markdown 格式表格中的所有课程信息；
                                 2. 将每门课按“天-时段-节次”拆分为单条记录；
                                 3. 严格输出 JSON 数组，不要包含多余文字。
                                 
                                 用户输入（User 提供）：
                                 以下是一个 Markdown 格式的课程表，请用它来提取课程信息：
                                 {markdown_table}
                                 
                                 任务说明（Prompt Body）：
                                 请解析上面提供的 Markdown 表格，按行——即每个“星期 + 时间段 + 节次 + 课程”——生成一条 JSON 记录。输出应满足以下要求：
                                 
                                 1. 输出格式 \s
                                    - 顶层应为一个 JSON 数组 `[]`，每个元素都是一个 JSON 对象 `{}`。 \s
                                    - 不要输出任何解释说明或额外字段。
                                 
                                 2. 每条记录字段 \s
                                    - `weekday`：星期几，格式 “星期一” 至 “星期日”。 \s
                                    - `time_slot`：时间段，取值 “上午”、“下午” 或 “晚上”。 \s
                                    - `period`：节次，如 “1-2 节”、“3-4 节”…… \s
                                    - `course_name`：课程名称，不包含后缀的星号或圈号。 \s
                                    - `course_type`：课程类型，取值 `"理论"`、`"实验"` 或 `"实践"`。 \s
                                    - `weeks`：上课周次，如 “1-10 周”、“双周”、“14 周”。 \s
                                    - `campus`：校区名称，如 “校本部”。 \s
                                    - `location`：具体场地，如 “管理楼305”；若为“未排地点”，填 `null`。 \s
                                    - `teacher`：授课教师姓名。 \s
                                    - `class_id`：教学班编号，如 “0007” 或 “0003B”。 \s
                                    - `students`：教学班组成学号列表，数组形式，如 `["2306804","2306805"]`。 \s
                                    - `assessment`：考核方式，如 “考试” 或 “考查”。 \s
                                    - `weekly_hours`：每周学时，数字类型。 \s
                                    - `credits`：学分，数字类型。 \s
                                 
                                 3. 合并单元格处理 \s
                                    - 对于 Markdown 中 `rowspan` 或 `colspan` 合并的单元格，需将该单元格内容分别复制到所有被合并的行/列中，确保每条记录都能单独完整地描述一节课。
                                 
                                 4. 示例输出 \s
                              
                                 [
                                   {
                                     "weekday": "星期二",
                                     "time_slot": "上午",
                                     "period": "1-2 节",
                                     "course_name": "计算机组成原理",
                                     "course_type": "理论",
                                     "weeks": "1-10 周",
                                     "campus": "校本部",
                                     "location": "管理楼305",
                                     "teacher": "李旎",
                                     "class_id": "0007",
                                     "students": ["2306804","2306805"],
                                     "assessment": "考试",
                                     "weekly_hours": 4,
                                     "credits": 3.5
                                   },
                                   {
                                     "weekday": "星期二",
                                     "time_slot": "上午",
                                     "period": "1-2 节",
                                     "course_name": "形势与政策",
                                     "course_type": "理论",
                                     "weeks": "14 周",
                                     "campus": "校本部",
                                     "location": "1 教110",
                                     "teacher": "佟志阳",
                                     "class_id": "0076",
                                     "students": ["2306803","2306804","2306805"],
                                     "assessment": "考查",
                                     "weekly_hours": 4,
                                     "credits": 2.0
                                   }
                             
                                 ]
                                 
            """;


    public void documentsloade(){
        //加载文件(通过自定义解析器来解析文件)
        List<Document> documents = FileSystemDocumentLoader.loadDocuments("documents", new ApacheTikaDocumentParser());

        //分成课表拆分和文本拆分
        for(Document document:documents){
            //判断是否为课表
            Boolean isCourseTable = checkIsCourseTable(document);
            if (isCourseTable){
                //对课表进行拆分
                ChatResponse chatResponse = chatLanguageModel.chat(SystemMessage.systemMessage(extractionPrompt), UserMessage.userMessage(documents.get(0).text()));
                log.info("课表解析结果：{}",chatResponse.aiMessage().text());
                String jsonList = chatResponse.aiMessage().text();
                try{
                    //将json转换成对象
                    ObjectMapper objectMapper=new ObjectMapper();
                    List<Course> courseTables = objectMapper.readValue(jsonList, new TypeReference<List<Course>>() {});
                    if(courseTables==null||courseTables.isEmpty()){
                        log.error("json转换对象失败");
                        continue;
                    }
                    //存储信息
                    List<TextSegment> collectList = courseTables.stream()
                            .map(course ->  TextSegment.from(course.toString()))
                            .collect(Collectors.toList());
                    //多维度存储到embeddingStore
                    List<Embedding> embeddingList = embeddingModel.embedAll(collectList).content();
                    embeddingStore.addAll(embeddingList, collectList);
                    //todo 后续将存储至本地数据库中

                }catch (Exception e){
                    log.error("存储失败");
                }
                continue;
            }
            DocumentByParagraphSplitter documentByParagraphSplitter = new DocumentByParagraphSplitter(700, 50, tokenizer);
            List<TextSegment> split = documentByParagraphSplitter.split(document);
            //对文本进行切分处理
            for(TextSegment textSegment : split) {
                Document segDoument = Document.from(textSegment.text());
                //对文本进行总结摘要
                ChatResponse chatResponse = chatLanguageModel.chat(SystemMessage.systemMessage("将该内容进行概括成只有 100 token的文本"), UserMessage.userMessage(segDoument.text()));
                //存入总结
                String content = chatResponse.aiMessage().text();
                //存储到本地
                Embedding embedding = embeddingModel.embed(content).content();
                embeddingStore.add(embedding, TextSegment.from(content));
                //将文本再切分成100token
                DocumentByParagraphSplitter stepDocumentSplitter = new DocumentByParagraphSplitter(100, 50, tokenizer);
                List<TextSegment> split1 = stepDocumentSplitter.split(segDoument);
                //存储到embeddingStore
                List<Embedding> embeddingList = embeddingModel.embedAll(split1).content();
                embeddingStore.addAll(embeddingList, split1);
            }
        }

    }




    /**
 * 评分系统
 * */
    private Boolean checkIsCourseTable(Document document) {

        //设置最低权重
        double  MIN_SCORE= 0.6;

        String text = document.text();
        //记录初始分数
        double score = 0;
        //计算权重
        if (text.contains("课表")){
            score+=0.3;
        }if (text.contains("教学班")){
            score+=0.2;
        }if (text.contains("星期")){
            score+=0.1;
        }if (text.contains("考核")){
            score+=0.1;
        }if( text.contains("学分")){
            score+=0.2;
        }if( text.contains("课程")){
            score+=0.2;
        }if (text.contains("时间")){
            score+=0.1;
        }if(text.contains("考核方式")){
            score+=0.1;
        }
        if (score>=MIN_SCORE){
            log.info("该文件是课表");
            return true;
        }else {
            log.info("该文件不是课表");
            return false;
        }


    }

    }