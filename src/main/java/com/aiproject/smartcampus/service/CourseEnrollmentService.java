package com.aiproject.smartcampus.service;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.po.CourseEnrollment;
import com.baomidou.mybatisplus.extension.service.IService;

public interface CourseEnrollmentService extends IService<CourseEnrollment> {

    Result<String> addCourseEnrollment(Integer courseId);

    Result<String> exitCourse(Integer courseId);
}
