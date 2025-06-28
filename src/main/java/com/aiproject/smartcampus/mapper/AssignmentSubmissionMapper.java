package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.po.AssignmentSubmission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

// 7. 作业提交Mapper
@Repository
@Mapper
public interface AssignmentSubmissionMapper extends BaseMapper<AssignmentSubmission> {
    
    /**
     * 查询作业提交列表
     */
    @Select("SELECT sub.*, s.student_number, u.real_name as student_name FROM assignment_submissions sub " +
            "LEFT JOIN students s ON sub.student_id = s.student_id " +
            "LEFT JOIN users u ON s.user_id = u.user_id " +
            "WHERE sub.assignment_id = #{assignmentId}")
    List<AssignmentSubmission> findByAssignmentId(@Param("assignmentId") Integer assignmentId);
    
    /**
     * 查询学生作业提交记录
     */
    @Select("SELECT sub.*, a.title, a.max_score, c.course_name FROM assignment_submissions sub " +
            "LEFT JOIN assignments a ON sub.assignment_id = a.assignment_id " +
            "LEFT JOIN courses c ON a.course_id = c.course_id " +
            "WHERE sub.student_id = #{studentId}")
    List<AssignmentSubmission> findByStudentId(@Param("studentId") Integer studentId);
    
    /**
     * 检查学生是否已提交作业
     */
    @Select("SELECT COUNT(*) FROM assignment_submissions WHERE assignment_id = #{assignmentId} AND student_id = #{studentId}")
    int checkSubmission(@Param("assignmentId") Integer assignmentId, @Param("studentId") Integer studentId);
}