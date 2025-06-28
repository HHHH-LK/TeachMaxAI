package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author lk_hhh
 */ // 15. 学生知识掌握表
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
    
    public enum MasteryLevel {
        NOT_LEARNED("not_learned"), LEARNING("learning"), MASTERED("mastered");
        
        private final String value;
        MasteryLevel(String value) { this.value = value; }
        public String getValue() { return value; }
    }
}