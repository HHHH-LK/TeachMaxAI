package com.aiproject.smartcampus.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;


/**
 * 章节资料统计信息DTO
 *
 * <p>用于统计和展示特定章节的资料相关数据，包括资料数量和总时长等信息</p>
 *
 * @author AI Project Team
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(description = "章节资料统计信息")
public class ChapterMaterialStats {

    /**
     * 章节ID
     *
     * <p>用于标识具体的章节，必须是有效的正整数</p>
     */
    @Schema(description = "章节ID", example = "1")
    private Integer chapterId;

    /**
     * 资料数量
     *
     * <p>该章节包含的资料总数，包括视频、文档、图片等各种类型的学习资料</p>
     */
    @Schema(description = "资料数量", example = "15")
    private Integer materialCount;

    /**
     * 总时长
     *
     * <p>该章节所有资料的预计学习时长总和，单位为分钟</p>
     * <p>主要用于帮助学生评估学习该章节所需的时间</p>
     */
    @Schema(description = "总时长(分钟)", example = "120")
    private Integer totalTime;
}
