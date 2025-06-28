package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


// 16. 学习统计表
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("learning_statistics")
public class LearningStatistics implements Serializable {
    
    @TableId(value = "stat_id", type = IdType.AUTO)
    private Integer statId;
    
    @TableField("user_id")
    private Integer userId;
    
    @TableField("date")
    private LocalDate date;
    
    @TableField("login_count")
    private Integer loginCount;
    
    @TableField("study_duration_minutes")
    private Integer studyDurationMinutes;
    
    @TableField("assignments_completed")
    private Integer assignmentsCompleted;
    
    @TableField("practice_count")
    private Integer practiceCount;
    
    // 关联用户信息
    @TableField(exist = false)
    private User user;
}
