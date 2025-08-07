package com.aiproject.smartcampus.service;

import com.aiproject.smartcampus.pojo.dto.StudentExamAnswerDTO;
import com.aiproject.smartcampus.pojo.po.Student;
import com.aiproject.smartcampus.pojo.vo.StudentSelectAllVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @program: SmartCampus
 * @description: 用户服务层
 * @author: lk
 * @create: 2025-05-20 08:37
 **/

public interface StudentService extends IService<Student> {

    List<StudentSelectAllVO> selectAllStudents();

    Student findByStudentNumber(String studentNumber);

    List<Student> findByClassName(String className);

    void updateStudent(Student student);

    String academicAnalysis(String courseId);

    void finshExam(StudentExamAnswerDTO studentExamAnswerDTO);
}
