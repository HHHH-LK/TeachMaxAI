package com.aiproject.smartcampus.mapper;


import com.aiproject.smartcampus.pojo.po.LessonPlan;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

// LessonPlanMapper.java
@Mapper
public interface LessonPlanMapper extends BaseMapper<LessonPlan> {

    /**
     * 查询待审核教案（带关联信息）
     */
    @Select("SELECT lp.*, c.course_name, ch.chapter_name, u.real_name as teacher_name " +
            "FROM lesson_plans lp " +
            "LEFT JOIN courses c ON lp.course_id = c.course_id " +
            "LEFT JOIN chapters ch ON lp.chapter_id = ch.chapter_id " +
            "LEFT JOIN teachers t ON lp.teacher_id = t.teacher_id " +
            "LEFT JOIN users u ON t.user_id = u.user_id " +
            "WHERE lp.audit_status = 'pending' " +
            "ORDER BY lp.created_at ASC")
    List<LessonPlan> findPendingAuditPlans();

    /**
     * 查询教师的教案列表
     */
    @Select("SELECT lp.*, c.course_name, ch.chapter_name " +
            "FROM lesson_plans lp " +
            "LEFT JOIN courses c ON lp.course_id = c.course_id " +
            "LEFT JOIN chapters ch ON lp.chapter_id = ch.chapter_id " +
            "WHERE lp.teacher_id = #{teacherId} " +
            "ORDER BY lp.updated_at DESC")
    List<LessonPlan> findByTeacherId(@Param("teacherId") Integer teacherId);

    /**
     * 查询教案详情（带关联信息）
     */
    @Select("SELECT lp.*, c.course_name, ch.chapter_name, " +
            "u1.real_name as teacher_name, u2.real_name as admin_name " +
            "FROM lesson_plans lp " +
            "LEFT JOIN courses c ON lp.course_id = c.course_id " +
            "LEFT JOIN chapters ch ON lp.chapter_id = ch.chapter_id " +
            "LEFT JOIN teachers t ON lp.teacher_id = t.teacher_id " +
            "LEFT JOIN users u1 ON t.user_id = u1.user_id " +
            "LEFT JOIN admins a ON lp.audit_admin_id = a.admin_id " +
            "LEFT JOIN users u2 ON a.user_id = u2.user_id " +
            "WHERE lp.plan_id = #{planId}")
    LessonPlan findDetailById(@Param("planId") Long planId);
}