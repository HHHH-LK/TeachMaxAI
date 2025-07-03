package com.aiproject.smartcampus.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;


/**
 * 课程资料统计信息DTO
 *
 * <p>用于统计和展示整个课程的资料概览信息，包括资料来源分类和总时长统计</p>
 * <p>主要用于课程管理页面的数据展示和分析</p>
 *
 * @author AI Project Team
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(description = "课程资料统计信息")
public class CourseMaterialStats {

    /**
     * 总资料数量
     *
     * <p>课程中所有资料的总数，等于教师资料数量+外部资料数量</p>
     */
    @Schema(description = "总资料数量", example = "50")
    private Integer totalMaterials;

    /**
     * 教师资料数量
     *
     * <p>由教师上传或创建的资料数量，包括课件、讲义、作业等</p>
     */
    @Schema(description = "教师资料数量", example = "30")
    private Integer teacherMaterials;

    /**
     * 外部资料数量
     *
     * <p>来自外部源的资料数量，如参考文献、网络资源链接等</p>
     */
    @Schema(description = "外部资料数量", example = "20")
    private Integer externalMaterials;

    /**
     * 总预计时长
     *
     * <p>完成整个课程所有资料学习的预计时长，单位为分钟</p>
     * <p>用于帮助学生进行学习计划安排</p>
     */
    @Schema(description = "总预计时长(分钟)", example = "600")
    private Integer totalEstimatedTime;
}