package com.aiproject.smartcampus.pojo.vo;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamStudentVO {
    private String examId; // 考试ID
    private String examName; // 考试名称
    private String courseId; // 课程ID
    private String courseName; // 课程名称
    private String studentId; // 学生ID
    private String studentName; // 学生姓名
    private String studentNumber; // 学生学号
    private Double score; // 分数
}
