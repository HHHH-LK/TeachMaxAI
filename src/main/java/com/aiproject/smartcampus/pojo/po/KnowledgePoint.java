package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author lk_hhh
 */
// 11. 知识点表
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("knowledge_points")
public class KnowledgePoint implements Serializable {

    @TableId(value = "point_id", type = IdType.AUTO)
    private Integer pointId;

    @TableField("course_id")
    private Integer courseId;

    @TableField("point_name")
    private String pointName;

    @TableField("description")
    private String description;

    @TableField("difficulty_level")
    private DifficultyLevel difficultyLevel;

    @TableField("parent_point_id")
    private Integer parentPointId;

    @TableField("keywords")
    private String keywords;

    // 关联信息
    @TableField(exist = false)
    private Course course;

    @TableField(exist = false)
    private KnowledgePoint parentPoint;

    @TableField(exist = false)
    private List<KnowledgePoint> childPoints;

    /**
     * 难度等级枚举
     */
    public enum DifficultyLevel {
        EASY("easy", "简单"),
        MEDIUM("medium", "中等"),
        HARD("hard", "困难");

        @EnumValue  // 告诉MyBatis-Plus使用这个字段的值与数据库交互
        @JsonValue  // JSON序列化时返回这个值
        private final String value;
        private final String description;

        DifficultyLevel(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public String getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        /**
         * 根据数据库值获取枚举
         */
        public static DifficultyLevel fromValue(String value) {
            for (DifficultyLevel level : DifficultyLevel.values()) {
                if (level.getValue().equals(value)) {
                    return level;
                }
            }
            return DifficultyLevel.MEDIUM; // 默认返回中等难度
        }
    }


}