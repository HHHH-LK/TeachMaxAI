package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.commons.utils.UserLocalThreadUtils;
import com.aiproject.smartcampus.commons.utils.UserToTypeUtils;
import com.aiproject.smartcampus.exception.StudentExpection;
import com.aiproject.smartcampus.mapper.CourseEnrollmentMapper;
import com.aiproject.smartcampus.mapper.CourseMapper;
import com.aiproject.smartcampus.pojo.po.Course;
import com.aiproject.smartcampus.pojo.po.User;
import com.aiproject.smartcampus.pojo.vo.CourseVO;
import com.aiproject.smartcampus.service.CourseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.aiproject.smartcampus.contest.CourseContest.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    private final CourseMapper courseMapper;
    private final CourseEnrollmentMapper courseEnrollmentMapper;
    private final UserToTypeUtils userToTypeUtils;

    /**
     * 查询所有课程信息
     *
     * @return
     */
    @Override
    public Result<List<Course>> findAllCourses() {
        List<Course> courses = courseMapper.findAllCourse();
        return Result.success(courses);
    }

    /**
     * 更新课程信息
     *
     * @param course
     * @return
     */
    @Override
    public Result<String> updateCourse(Course course) {
        User.UserType userType = UserLocalThreadUtils.getUserInfo().getUserType();
        //参数校验
        //用户信息是否存在
        if (userType.toString().isBlank()) {
            return Result.error(ERROR_USERINFO);
        }
        //权限校验
        if (!"admin".equals(userType)) {
            return Result.error(NO_PERMISSION_UPDATE);
        }
        //课程参数校验
        if (course.getCourseId() == null) {
            return Result.error(NO_EXIST_COURSE_ID);
        }
        if (course.getCourseName().isBlank()) {
            return Result.error(NO_EXIST_COURSE_NAME);
        }

        courseMapper.updateById(course);
        return Result.success(SUCCESS_UPDATE_COURSE);
    }

    @Override
    public Result<String> deleteCourse(Integer courseId) {
        // todo 校验用户权限
        User.UserType userType = UserLocalThreadUtils.getUserInfo().getUserType();
        //权限校验
        if (!"admin".equals(userType)) {
            return Result.error(NO_PERMISSION_DELETE);
        }

        //查询课程是否存在
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            return Result.error(NO_EXIST_COURSE);
        }
        //删除课程
        courseMapper.deleteById(courseId);
        return Result.success(SUCCESS_DELETE_COURSE);
    }

    @Override
    public Result<String> addCourse(Course course) {
        // todo 校验用户权限
        User.UserType userType = UserLocalThreadUtils.getUserInfo().getUserType();
        //权限校验
        if (!"admin".equals(userType)) {
            return Result.error(NO_PERMISSION_ADD);
        }

        //判断课程是否存在
        LambdaQueryWrapper<Course> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(!course.getCourseName().isBlank(), Course::getCourseName, course.getCourseName());
        Long count = courseMapper.selectCount(queryWrapper);
        if (count > 0) {
            return Result.error(HAVING_ADD_COURSE);
        }

        //添加课程
        courseMapper.insert(course);
        return Result.success(SUCCESS_UPDATE_COURSE);
    }


    @Override
    public Result<List<CourseVO>> findAllCoursesByDate(String date) {

        String studentId = userToTypeUtils.change();

        List<CourseVO> allCourseByDate = courseMapper.findAllCourseByDate(date, studentId);
        for (CourseVO course : allCourseByDate) {
            //添加课程描述
            String courseDescription = CourseVO.getCourseDescription(course.getCourseName());
            log.info("获取课程{}描述{}", course.getCourseName(), courseDescription);
            course.setCourseDescription(courseDescription);
        }

        return Result.success(allCourseByDate);
    }

    @Override
    public Result<List<CourseVO>> getAllStudentHaveCourse() {

        /*String studentId = userToTypeUtils.change();*/
        String studentId = "1";

        List<CourseVO> allCourseByByStudent = courseMapper.findAllCourseByStudentId(studentId);
        for (CourseVO course : allCourseByByStudent) {
            //添加课程描述
            String courseDescription = CourseVO.getCourseDescription(course.getCourseName());
            log.info("获取课程{}描述{}", course.getCourseName(), courseDescription);
            course.setCourseDescription(courseDescription);
        }

        return Result.success(allCourseByByStudent);
    }

    @Override
    public Result<List<String>> getAllLearnDate() {

        String studentId = userToTypeUtils.change();

        List<String> studentSemesters = courseMapper.getStudentSemesters(Integer.valueOf(studentId));

        if (studentSemesters == null || studentSemesters.isEmpty()) {

            throw new StudentExpection("学生未有学期选课");

        }

        return Result.success(studentSemesters);
    }


}

