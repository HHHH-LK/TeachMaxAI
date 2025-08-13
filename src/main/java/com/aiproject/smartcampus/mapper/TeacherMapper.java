package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.dto.TeacherInfoDTO;
import com.aiproject.smartcampus.pojo.dto.TeacherQueryDTO;
import com.aiproject.smartcampus.pojo.po.Teacher;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
     * 根据部门查询教师列表
     */
    @Select("SELECT t.*, u.real_name FROM teachers t LEFT JOIN users u ON t.user_id = u.user_id WHERE t.department = #{department}")
    List<Teacher> findByDepartment(@Param("department") String department);

    @Select("SELECT t.*, u.real_name FROM teachers t LEFT JOIN users u ON t.user_id = u.user_id WHERE t.teacher_id = #{teacher_id}")
    Teacher findByTeacherID(@Param("teacher_id") Integer teacherId);

    /**
     * 查询老师的所有信息
     * */
    @Select("SELECT \n" +
            "    t.teacher_id,\n" +
            "    t.employee_number,\n" +
            "    t.department,\n" +
            "    u.real_name,\n" +
            "    u.email,\n" +
            "    u.phone\n" +
            "FROM teachers t\n" +
            "INNER JOIN users u ON t.user_id = u.user_id\n" +
            "WHERE t.teacher_id = #{teacherId};")
    TeacherInfoDTO findTeacherInfoById(String teacherId);



    @Select("SELECT t.*, u.* FROM teachers t LEFT JOIN users u ON t.user_id = u.user_id WHERE u.user_id = #{userId}")
    @Results({
            @Result(property = "teacherId", column = "teacher_id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "employee_number", column = "employee_number"),
            @Result(property = "department", column = "department"),
            @Result(property = "username", column = "username"),
            @Result(property = "real_name", column = "real_name"),
            @Result(property = "email", column = "email"),
            @Result(property = "phone", column = "phone")
    })
    TeacherQueryDTO findByUserID(@Param("userId") Integer userId);

    @Select("SELECT DISTINCT ce.student_id\n" +
            "FROM courses c\n" +
            "INNER JOIN course_enrollments ce ON c.course_id = ce.course_id\n" +
            "WHERE c.teacher_id = #{teacherId} AND c.course_id = #{courseId}; -- 替换为具体的老师ID和课程ID")
    List<Integer> selectAllClassStudentInfo(@Param("teacherId")String teacherId, @Param("courseId")String courseId);

    @Update("UPDATE users u JOIN teachers t ON u.user_id = t.user_id " +
            "SET u.username = #{username}, u.real_name = #{real_name}, " +
            "u.email = #{email}, u.phone = #{phone}, " +
            "t.employee_number = #{employee_number}, t.department = #{department} " +
            "WHERE t.user_id = #{userId}")
    int updateTeacherProfile(TeacherQueryDTO dto);

    @Delete("DELETE FROM teachers WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Integer userId);

    @Select("SELECT * FROM teachers WHERE user_id = #{userId}")
    Teacher selectByUserId(@Param("userId") Integer userId);

    @Select("select teachers.user_id from teachers where teacher_id=#{teacherId} ")
    Integer getUserIdByTeacherId(Integer teacherId);

    @Select("SELECT * FROM teachers WHERE department = #{department}")
    List<Teacher> findByRelatedDepartment(@Param("department") String department);

    @Select("SELECT * FROM teachers")
    List<Teacher> findAll();
}
