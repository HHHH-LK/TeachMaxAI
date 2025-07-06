package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.bo.KnowledgepointBO;


import com.aiproject.smartcampus.pojo.po.StudentKnowledgeMastery;
import com.aiproject.smartcampus.pojo.po.Teacher;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author lk_hhh
 */ // 13. 学生知识掌握Mapper
@Repository
@Mapper
public interface StudentKnowledgeMasteryMapper extends BaseMapper<StudentKnowledgeMastery> {

    /**
     * 查询学生知识点掌握情况
     */
    @Select("SELECT skm.*, kp.point_name, kp.difficulty_level FROM student_knowledge_mastery skm " +
            "LEFT JOIN knowledge_points kp ON skm.point_id = kp.point_id " +
            "WHERE skm.student_id = #{studentId}")
    List<StudentKnowledgeMastery> findByStudentId(@Param("studentId") Integer studentId);

    /**
     * 查询课程知识点掌握情况
     */
    @Select("SELECT skm.*, kp.point_name FROM student_knowledge_mastery skm " +
            "LEFT JOIN knowledge_points kp ON skm.point_id = kp.point_id " +
            "WHERE skm.student_id = #{studentId} AND kp.course_id = #{courseId}")
    List<StudentKnowledgeMastery> findByStudentAndCourse(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);

    /**
     * 知识点掌握统计
     */
    @Select("SELECT " +
            "mastery_level, " +
            "COUNT(*) as count " +
            "FROM student_knowledge_mastery skm " +
            "LEFT JOIN knowledge_points kp ON skm.point_id = kp.point_id " +
            "WHERE skm.student_id = #{studentId} AND kp.course_id = #{courseId} " +
            "GROUP BY mastery_level")
    List<Map<String, Object>> getMasteryStatistics(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);

    /**
     * 查询未掌握的知识点
     */
    @Select("select point_name, description, point_id,course_id\n" +
            "from knowledge_points\n" +
            "where point_id in (select point_id\n" +
            "                   from student_knowledge_mastery\n" +
            "                   where student_id = #{studentId}\n" +
            "                     and mastery_level != 'mastered'\n" +
            "                     and course_id = #{courseId})")
    List<KnowledgepointBO> getNotMasterKnowledgepoints(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);

}