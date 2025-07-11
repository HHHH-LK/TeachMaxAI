package com.aiproject.smartcampus.mapper;

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


}