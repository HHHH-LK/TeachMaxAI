package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 3. 章节题目关系表实体类
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("chapter_questions")
public class ChapterQuestion {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    
    @TableField("chapter_id")
    private Integer chapterId;
    
    @TableField("question_id")
    private Integer questionId;
    
    @TableField("question_type")
    private QuestionType questionType;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    public enum QuestionType {
        PRACTICE("practice"),
        TEST("test"),
        EXAM("exam");

        @EnumValue  // 告诉MyBatis-Plus使用这个字段的值与数据库交互
        @JsonValue  // JSON序列化时返回这个值
        private final String value;
        
        QuestionType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static QuestionType fromValue(String value) {
            for (QuestionType type : values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            return PRACTICE; // 默认值
        }
    }
}