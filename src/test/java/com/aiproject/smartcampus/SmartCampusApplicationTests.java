package com.aiproject.smartcampus;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.commons.utils.PromptUtils;

import com.aiproject.smartcampus.service.TeacherAIservice;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.poi.xslf.usermodel.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.awt.*;
import java.io.*;
import java.util.List;

import static com.aiproject.smartcampus.model.prompts.SystemPrompts.INITENT_ANALYSIS_PROMPT;
import static com.aiproject.smartcampus.model.prompts.SystemPrompts.INITENT_SPIDLER_PROMPT;


@Slf4j
@SpringBootTest
class SmartCampusApplicationTests {

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Test
    void contextLoads() {


        String prompt = PromptUtils.buildAnswerPrompt("李明是猪", "李明是谁");
        String answer = chatLanguageModel.chat(prompt);
        System.out.println(answer);

    }

    @Test
    void test() {
        final  String extractionPrompt = """
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
                                 ```json
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
                                   // ……更多记录
                                 ]
                                 
            """;

        List<Document> documents = FileSystemDocumentLoader.loadDocuments("documents", new ApacheTikaDocumentParser());
          log.info(documents.toString());
        System.out.println(documents.get(0));
          ChatResponse chatResponse = chatLanguageModel.chat(SystemMessage.systemMessage(extractionPrompt), UserMessage.userMessage(documents.get(0).text()));
          System.out.println(chatResponse.aiMessage().text());
    }


    @Test
    void test2(){
        ChatResponse chatResponse = chatLanguageModel.chat(SystemMessage.from(INITENT_ANALYSIS_PROMPT), UserMessage.from("我课表中学分最高的三门课是那三门？"));
        String text = chatResponse.aiMessage().text();
        System.out.println(text);
        System.out.println("----------------------------");
        ChatResponse chatResponse1 = chatLanguageModel.chat(SystemMessage.from(INITENT_SPIDLER_PROMPT), UserMessage.from(text));
        System.out.println(chatResponse1.aiMessage().text());


    }


    @Test

    void test3(){
        ChatResponse chat = chatLanguageModel.chat(UserMessage.from("帮我生成一个关于保护环境的ppt"));
        System.out.println(chat.aiMessage().text());


    }


    @Test
    void test4() throws IOException {

            XMLSlideShow ppt = new XMLSlideShow();
            // 创建幻灯片
            XSLFSlide slide = ppt.createSlide();
            // 添加标题
            XSLFTextBox title = slide.createTextBox();
            title.setAnchor(new Rectangle(50, 50, 600, 50));
            title.setText("这是PPT标题");

            // 添加正文
            XSLFTextBox content = slide.createTextBox();
            content.setAnchor(new Rectangle(50, 150, 600, 300));
            content.setText("这里是PPT内容，支持多行文本");

            // 保存文件
            try (FileOutputStream out = new FileOutputStream("demo.pptx")) {
                ppt.write(out);
            }

            System.out.println("PPT生成成功！");
    }


    @Test
    void test5() throws IOException {

        ChatResponse chat = chatLanguageModel.chat(UserMessage.from("帮我生成一张垃圾分类的相关图片"));
        System.out.println(chat.aiMessage().text());


    }

    @Test
    void test6() throws IOException {

        ChatResponse chat = chatLanguageModel.chat(UserMessage.from("帮我为java程序设计这门课程添加一个课程描述"));
        System.out.println(chat.aiMessage().text());

    }


    @Autowired
    private TeacherAIservice teacherAIservice;

    @Test
    void test7() throws IOException {

        Result<String> stringResult = teacherAIservice.aiclassAiayaisc("1");
        log.info(stringResult.getData());
    }

    @Test
    void test8() throws IOException {

        Result<String> stringResult = teacherAIservice.TeacherTextCreate("生成一份高质量的教案，我要用来进行教学任务", "1", "1");
        log.info(stringResult.getData());

    }


        @Test
        void test9() throws IOException {

            Result<String> result = teacherAIservice.teacherCreateTest("帮我针对整个班级进行定制化的生成章节测试题", "1", "1");

            log.info(result.getData());


        }

        @Test
    void test10() throws IOException {

            File pdfFile = new File("/Users/lk_hhh/Desktop/Thinking+in+Java+4th+Edition（JAVA编程思想 第四版 英文版）.pdf");

            System.out.println("exists? " + pdfFile.exists());   // 应该打印 true
            System.out.println("canRead? " + pdfFile.canRead()); // 应该打印 true



        }



}
