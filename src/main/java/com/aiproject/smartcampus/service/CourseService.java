package com.aiproject.smartcampus.service;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.po.Course;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface CourseService extends IService<Course> {

    Result<List<Course>> findAllCourses();

    Result<String> updateCourse(Course course);

    Result<String> deleteCourse(Integer courseId);

    Result<String> addCourse(Course course);
}
