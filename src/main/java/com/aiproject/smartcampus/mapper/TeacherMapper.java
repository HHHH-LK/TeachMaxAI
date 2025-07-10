package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.po.Teacher;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.langchain4j.agent.tool.P;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * @author lk_hhh
 */ // 3. 教师Mapper
@Repository
@Mapper
public interface TeacherMapper extends BaseMapper<Teacher> {
    
    /**
     * 根据工号查询教师
     */
    @Select("SELECT t.*, u.* FROM teachers t LEFT JOIN users u ON t.user_id = u.user_id WHERE t.employee_number = #{employeeNumber}")
    @Results({
        @Result(property = "teacherId", column = "teacher_id"),
        @Result(property = "userId", column = "user_id"),
        @Result(property = "employeeNumber", column = "employee_number"),
        @Result(property = "department", column = "department"),
        @Result(property = "user.userId", column = "user_id"),
        @Result(property = "user.username", column = "username"),
        @Result(property = "user.realName", column = "real_name"),
        @Result(property = "user.email", column = "email"),
        @Result(property = "user.phone", column = "phone")
    })
    Teacher findByEmployeeNumber(@Param("employeeNumber") String employeeNumber);
    
    /**
     * 根据部门查询教师列表
     */
    @Select("SELECT t.*, u.real_name FROM teachers t LEFT JOIN users u ON t.user_id = u.user_id WHERE t.department = #{department}")
    List<Teacher> findByDepartment(@Param("department") String department);

    @Select("SELECT t.*, u.real_name FROM teachers t LEFT JOIN users u ON t.user_id = u.user_id WHERE t.teacher_id = #{teacher_id}")
    Teacher findByTeacherID(@Param("teacher_id") Integer teacherId);

    @Select("SELECT t.*, u.real_name FROM teachers t LEFT JOIN users u ON t.user_id = u.user_id WHERE t.user_id = #{user_id}")
    Teacher findByUserID(@Param("user_id") Long userId);

    @Select("SELECT DISTINCT ce.student_id\n" +
            "FROM courses c\n" +
            "INNER JOIN course_enrollments ce ON c.course_id = ce.course_id\n" +
            "WHERE c.teacher_id = #{teacherId} AND c.course_id = #{courseId}; -- 替换为具体的老师ID和课程ID")
    List<Integer> selectAllClassStudentInfo(@Param("teacherId")String teacherId, @Param("courseId")String courseId);
}
