package com.aiproject.smartcampus.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;


/**
 * 学生章节进度统计信息DTO
 *
 * <p>用于统计和展示学生在特定课程中的章节学习进度信息</p>
 * <p>主要用于学习进度跟踪、学习分析和个性化推荐</p>
 *
 * <p><strong>使用场景：</strong></p>
 * <ul>
 *   <li>学生个人学习进度页面</li>
 *   <li>教师对学生学习情况的统计分析</li>
 *   <li>学习报告生成</li>
 *   <li>学习效果评估</li>
 * </ul>
 *
 * @author AI Project Team
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(description = "学生章节进度统计信息")
public class StudentChapterProgressStats {

    /**
     * 总章节数
     *
     * <p>课程中的章节总数，用于计算学习进度的基数</p>
     */
    @Schema(description = "总章节数", example = "10")
    private Integer totalChapters;

    /**
     * 已完成章节数
     *
     * <p>学生已经完成学习的章节数量</p>
     * <p>完成标准：章节内所有必修内容都已学习完成</p>
     */
    @Schema(description = "已完成章节数", example = "7")
    private Integer completedChapters;

    /**
     * 平均进度
     *
     * <p>所有章节的平均完成进度，以百分比表示</p>
     * <p>计算方式：(各章节进度之和) / 总章节数</p>
     * <p>取值范围：0.0 - 100.0</p>
     */
    @Schema(description = "平均进度(百分比)", example = "75.5")
    private Double avgProgress;

    /**
     * 总学习时长
     *
     * <p>学生在所有章节上累计的学习时长，单位为分钟</p>
     * <p>包括视频观看、文档阅读、练习完成等各种学习活动的时间</p>
     */
    @Schema(description = "总学习时长(分钟)", example = "480")
    private Integer totalStudyTime;

    /**
     * 获取完成率
     *
     * <p>计算已完成章节占总章节的百分比</p>
     *
     * @return 完成率百分比，如果总章节数为0则返回0.0
     */
    public Double getCompletionRate() {
        if (totalChapters == null || totalChapters == 0) {
            return 0.0;
        }
        if (completedChapters == null) {
            return 0.0;
        }
        return (completedChapters.doubleValue() / totalChapters.doubleValue()) * 100;
    }

    /**
     * 获取平均每章节学习时长
     *
     * <p>计算每个章节的平均学习时长</p>
     *
     * @return 平均每章节学习时长(分钟)，如果总章节数为0则返回0.0
     */
    public Double getAvgTimePerChapter() {
        if (totalChapters == null || totalChapters == 0) {
            return 0.0;
        }
        if (totalStudyTime == null) {
            return 0.0;
        }
        return totalStudyTime.doubleValue() / totalChapters.doubleValue();
    }
}