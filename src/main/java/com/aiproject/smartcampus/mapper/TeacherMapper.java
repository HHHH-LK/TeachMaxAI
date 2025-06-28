package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.bo.classprase.Course;
import com.aiproject.smartcampus.pojo.po.Exam;
import com.aiproject.smartcampus.pojo.po.ExamScore;
import com.aiproject.smartcampus.pojo.po.Teacher;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


// 3. 教师Mapper
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
}
