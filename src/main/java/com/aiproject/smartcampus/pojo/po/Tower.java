package com.aiproject.smartcampus.pojo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @program: TeacherMaxAI
 * @description: 塔实体
 * @author: lk_hhh
 * @create: 2025-08-08 13:37
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("tower")
public class Tower implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 塔ID，主键，自增
     */
    @TableId(value = "tower_id", type = IdType.AUTO)
    private Long towerId;

    /**
     * 所属课程ID（可与教学系统关联）
     */
    @TableField(value = "course_id")
    private Long courseId;

    /**
     * 总层数
     */
    @TableField(value = "total_floors")
    private Integer totalFloors;

    /**
     * 塔名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 塔描述（故事背景、主题等）
     */
    @TableField(value = "description")
    private String description;

    /**
     * 所属学生id
     */
    @TableField(value = "student_id")
    private Long studentId;

}
