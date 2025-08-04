package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.dto.StudentAnswerDTO;
import com.aiproject.smartcampus.pojo.dto.StudentExamAnswerDTO;
import com.aiproject.smartcampus.pojo.po.Exam;
import com.aiproject.smartcampus.pojo.po.Student;
import com.aiproject.smartcampus.pojo.vo.StudentSelectAllVO;
import com.aiproject.smartcampus.service.StudentService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stu")
@Slf4j
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping("/students")
    @ApiOperation(value = "查询所有学生信息", notes = "查询所有学生信息")
    public Result<List<StudentSelectAllVO>> selectAllStudents() {
        List<StudentSelectAllVO> students = studentService.selectAllStudents();
        return Result.success(students);
    }

    /**
     * 根据学生学号查询学生信息
     *
     * @param studentNumber 学生ID
     */
    @GetMapping("/student/{studentNumber}")
    @ApiOperation(value = "根据学号查询学生信息", notes = "查询学生信息")
    public Result<Student> findByStudentNumber(@PathVariable("studentNumber") String studentNumber) {
        Student student = studentService.findByStudentNumber(studentNumber);
        log.info("查询学生信息成功，student={}", student);
        return Result.success(student);
    }

    /**
     * 根据班级名称查询班级学生信息
     *
     * @param className
     * @return
     */
    @GetMapping("/student/class")
    @ApiOperation(value = "查询班级学生信息", notes = "查询班级学生信息")
    public Result<List<Student>> findByClassName(@RequestParam("className") String className) {
        List<Student> students = studentService.findByClassName(className);
        return Result.success(students);
    }

    /**
     * 更新学生信息
     *
     * @param student
     * @return
     */
    @ApiOperation(value = "更新学生信息", notes = "更新学生信息")
    @PutMapping("/update")
    public Result updateStudent(@RequestBody Student student) {
        studentService.updateStudent(student);
        return Result.success();
    }

    /**
     * 学生学情分析//todo 基于课程的所有错误知识进行学情分析
     */
    @ApiOperation(value = "ai智能生成学情分析", notes = "学情情况分析")
    @GetMapping("/academicanalysis")
    public Result<String> academicAnalysis(@RequestParam(value = "courseId") String courseId) {

        String result = studentService.academicAnalysis(courseId);

        return Result.success(result);
    }

    /**
     * 学生提交试卷
     */
    @ApiOperation(value = "学生试卷答题功能", notes = "学生试卷答题")
    @PostMapping("/finsh/exam")
    public Result finshExam(@RequestBody StudentExamAnswerDTO studentExamAnswerDTO) {

        studentService.finshExam(studentExamAnswerDTO);

        return Result.success();
    }


}


















