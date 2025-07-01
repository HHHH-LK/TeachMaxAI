package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author lk_hhh
 */
// 15. 学生知识掌握表
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("student_knowledge_mastery")
public class StudentKnowledgeMastery implements Serializable {

    @TableId(value = "mastery_id", type = IdType.AUTO)
    private Integer masteryId;

    @TableField("student_id")
    private Integer studentId;

    @TableField("point_id")
    private Integer pointId;

    @TableField("mastery_level")
    private MasteryLevel masteryLevel;

    @TableField("practice_score")
    private BigDecimal practiceScore;

    @TableField("practice_count")
    private Integer practiceCount;

    @TableField(value = "last_updated", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastUpdated;

    // 关联信息
    @TableField(exist = false)
    private Student student;

    @TableField(exist = false)
    private KnowledgePoint knowledgePoint;

    /**
     * 掌握程度枚举
     */
    public enum MasteryLevel {
        NOT_LEARNED("not_learned", "未学习"),
        LEARNING("learning", "学习中"),
        MASTERED("mastered", "已掌握");

        @EnumValue  // 告诉MyBatis-Plus使用这个字段的值与数据库交互
        @JsonValue  // JSON序列化时返回这个值
        private final String value;
        private final String description;

        MasteryLevel(String value, String description) {
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
        public static MasteryLevel fromValue(String value) {
            for (MasteryLevel level : MasteryLevel.values()) {
                if (level.getValue().equals(value)) {
                    return level;
                }
            }
            return MasteryLevel.NOT_LEARNED; // 默认返回未学习
        }
    }


}