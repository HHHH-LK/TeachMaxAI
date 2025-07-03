package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 2. 章节知识点关系表实体类
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("chapter_knowledge_points")
public class ChapterKnowledgePoint {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    
    @TableField("chapter_id")
    private Integer chapterId;
    
    @TableField("point_id")
    private Integer pointId;
    
    @TableField("point_order")
    private Integer pointOrder;
    
    @TableField("is_core")
    private Boolean isCore;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}