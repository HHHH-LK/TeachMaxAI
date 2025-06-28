package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author lk_hhh
 */ // 11. 知识点表
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
    
    public enum DifficultyLevel {
        EASY("easy"), MEDIUM("medium"), HARD("hard");
        
        private final String value;
        DifficultyLevel(String value) { this.value = value; }
        public String getValue() { return value; }
    }
}