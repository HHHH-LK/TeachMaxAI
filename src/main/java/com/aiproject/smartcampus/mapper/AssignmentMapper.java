package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.po.Assignment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * @author lk_hhh
 */ // 6. 作业Mapper
@Repository
@Mapper
public interface AssignmentMapper extends BaseMapper<Assignment> {
    
    /**
     * 查询课程作业列表
     */
    @Select("SELECT * FROM assignments WHERE course_id = #{courseId} ORDER BY created_at DESC")
    List<Assignment> findByCourseId(@Param("courseId") Integer courseId);
    
    /**
     * 查询学生待完成作业
     */
    @Select("SELECT a.* FROM assignments a " +
            "LEFT JOIN course_enrollments ce ON a.course_id = ce.course_id " +
            "LEFT JOIN assignment_submissions sub ON a.assignment_id = sub.assignment_id AND sub.student_id = ce.student_id " +
            "WHERE ce.student_id = #{studentId} AND a.status = 'published' " +
            "AND sub.submission_id IS NULL AND a.due_date > NOW()")
    List<Assignment> findPendingAssignments(@Param("studentId") Integer studentId);
    
    /**
     * 作业统计信息
     */
    @Select("SELECT a.assignment_id, a.title, " +
            "COUNT(sub.submission_id) as submission_count, " +
            "COUNT(ce.student_id) as total_students, " +
            "AVG(sub.score) as avg_score " +
            "FROM assignments a " +
            "LEFT JOIN course_enrollments ce ON a.course_id = ce.course_id " +
            "LEFT JOIN assignment_submissions sub ON a.assignment_id = sub.assignment_id " +
            "WHERE a.assignment_id = #{assignmentId} " +
            "GROUP BY a.assignment_id")
    Map<String, Object> getAssignmentStatistics(@Param("assignmentId") Integer assignmentId);
}