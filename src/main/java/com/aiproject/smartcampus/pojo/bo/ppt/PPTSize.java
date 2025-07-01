package com.aiproject.smartcampus.pojo.bo.ppt;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @program: SmartCampus
 * @description: ppt样式设置参数
 * @author: lk_hhh
 * @create: 2025-06-29 16:20
 **/

@Data
public class PPTSize {

    /**
     * PPT 的主题，例如：“AI 在医疗领域的应用”
     */
    private String topic;

    /**
     * 补充描述：可包括用途、对象、演讲时长、风格等。例如：“用于公司展示，10分钟，偏学术风”
     */
    private String description;

    /**
     * 期望生成的幻灯片页数，如果为空，系统自动估算
     */
    private Integer pages;

    /**
     * 输出语言，例如 "zh"、"en"，默认为中文
     */
    private String language = "zh";

    /**
     * 表达风格，例如 "formal"（正式）、"casual"（轻松）、"academic"（学术）
     */
    private String tone = "formal";

    /**
     * 是否需要自动插入图表（如柱状图、折线图等）
     */
    private boolean needCharts = true;

    /**
     * 是否需要自动插入配图（支持 AI 配图或图搜提示词）
     */
    private boolean needImages = true;

    /**
     * 输出格式，支持 pptx（默认）、pdf、html 等
     */
    private String outputFormat = "pptx";

    /**
     * 自定义目录结构（如用户指定了内容章节标题）
     */
    private List<String> customOutline;

    /**
     * 上传的文件，例如 Word 文档、PDF 或 TXT，用于从中提取内容（可选）
     */
    private MultipartFile uploadFile;
}