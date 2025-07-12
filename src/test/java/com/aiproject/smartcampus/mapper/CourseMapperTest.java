package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.vo.CourseVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class CourseMapperTest {

    @Autowired
    private CourseMapper courseMapper;

    @Test
    void getCourseById() {

        List<String> studentSemesters = courseMapper.getStudentSemesters(1);
        System.out.println(studentSemesters);

    }


    @Test
    void getCourseByCourseId() {

        List<CourseVO> allCourseByByStudent = courseMapper.findAllCourseByStudentId("1");
        System.out.println(allCourseByByStudent);

    }

}