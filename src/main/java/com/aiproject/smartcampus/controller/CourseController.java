package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.po.Course;
import com.aiproject.smartcampus.pojo.vo.ChapterQuestionDetailVO;
import com.aiproject.smartcampus.pojo.vo.CourseVO;
import com.aiproject.smartcampus.pojo.vo.ExamQuestionDetailVO;
import com.aiproject.smartcampus.service.CourseEnrollmentService;
import com.aiproject.smartcampus.service.CourseService;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@Slf4j
@RequestMapping("/course")
@Api(tags = "课表相关接口")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final CourseEnrollmentService courseEnrollmentService;

    /**
     * 获取所有课表(包含教师信息)
     * @return
     */
    @GetMapping("/all")
    @Operation(summary = "获取所有课表")
    public Result<List<Course>> findAllCourses(){
        return courseService.findAllCourses();
    }

    /**
     * 更新课表
     * @return
     */
    @PutMapping("/update/course")
    @Operation(summary = "更新课表")
    public Result<String> updateCourse(@RequestBody Course course){
        return courseService.updateCourse(course);
    }

    /**
     * 删除课程
     * @param courseId
     * @return
     */
    @DeleteMapping("/delete/{courseId}")
    @Operation(summary = "删除课程")
    public Result<String> deleteCourse(@PathVariable("courseId") Integer courseId){
        return courseService.deleteCourse(courseId);
    }

    /**
     * 添加课程
     * @param course
     * @return
     */
    @PostMapping("/add/course")
    @Operation(summary = "添加课程")
    public Result<String> addCourse(@RequestBody Course course){
        return courseService.addCourse(course);
    }


    /**
     * 添加选课记录
     */
    @PostMapping("/add/{courseId}")
    @Operation(summary = "添加选课记录")
    public Result<String> addEnrollment(@PathVariable("courseId") Integer courseId){
       return courseEnrollmentService.addCourseEnrollment(courseId);
    }

    /**
     * 退选课程
     * */
    @PostMapping("/exit/{courseId}")
    @Operation(summary = "退选课程")
    public Result<String> exitCourse(@PathVariable("courseId") Integer courseId){
        return courseEnrollmentService.exitCourse(courseId);
    }


    /**
     * 查询课程考试信息
     * */
    @GetMapping("/getCourseExamInfo")
    @Operation(summary = "查询课程考试信息题目")
    public Result<List<ExamQuestionDetailVO>> getCourseExamInfo(
            @RequestParam(value = "examId") String examId){
        return courseService.getCourseExamInfo(examId);
    }

    /**
     * 查询学生考试信息
     * */
    @GetMapping("/getCourseExamStudent")
    @Operation(summary = "查询学生考试信息")
    public Result<List<ExamQuestionDetailVO>> getCourseExamInfo(
            @RequestParam(value = "examId") String examId, @RequestParam(value = "studentId") String studentId){
        return courseService.getCourseExamStudent(examId, studentId);
    }

    /**
     * 根据年份学生查询课程信息
     * */
    @GetMapping("/getAllCourse")
    public Result<List<CourseVO>> getAllCourse(@RequestParam(value = "date")String date ){

        return courseService.findAllCoursesByDate(date);

    }

    /**
     * 查询学生选课的所有记录
     * */
    @GetMapping("/getAllStudentHaveCourse")
    public Result<List<CourseVO>> getAllStudentHaveCourse(){

        return courseService.getAllStudentHaveCourse();
    }

    /**
     * 查询所有已有的学年
     * */
    @GetMapping("/getAllLearnDate")
    public Result<List<String>> getAllLearnDate(){

        return courseService.getAllLearnDate();
    }

    /**
     * 查询课程作业信息
     */
    @GetMapping("/getCourseHomeworkInfo")
    @Operation(summary = "查询课程作业信息")
    public Result getCourseHomeworkInfo(
            @RequestParam(value = "courseId") String courseId) {
        return courseService.getCourseHomeworkInfo(courseId);
    }

    /**
     * 智能创建课程
     */
    @PostMapping("/createCourse")
    @Operation(summary = "智能创建课程")
    public Result<String> createCourse(String courseName, String teacherId, String semester) {
        return courseService.createCourse(courseName, teacherId, semester);
    }

    /**
     * 更换课程教师
     */
    @PutMapping("/changeTeacher")
    @Operation(summary = "更换课程教师")
    public Result<String> changeTeacher(@RequestParam("courseId") Integer courseId, @RequestParam("teacherId") String teacherId) {
        return courseService.changeTeacher(courseId, teacherId);
    }

}
