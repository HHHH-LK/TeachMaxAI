package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.vo.CourseVO;
import com.aiproject.smartcampus.service.CourseService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Slf4j
class CourseServiceImplTest {

    @Autowired
    private CourseService courseService;

    @Test
    void getCourseById() {

        Result<List<CourseVO>> allCoursesByDate = courseService.findAllCoursesByDate("2024春季");
        List<CourseVO> data = allCoursesByDate.getData();
        log.info(data.toString());

    }



    @Test
    void getCourseById2() {

        Result<List<CourseVO>> allStudentHaveCourse = courseService.getAllStudentHaveCourse();
        List<CourseVO> data = allStudentHaveCourse.getData();
        log.info(data.toString());


    }


}