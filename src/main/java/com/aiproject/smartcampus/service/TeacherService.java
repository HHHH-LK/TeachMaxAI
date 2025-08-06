package com.aiproject.smartcampus.service;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.bo.StudentWrongKnowledgeBO;
import com.aiproject.smartcampus.pojo.dto.TeacherGetSituationDTO;
import com.aiproject.smartcampus.pojo.dto.TeacherGetStudentDTO;
import com.aiproject.smartcampus.pojo.dto.TeacherQueryDTO;
//import com.aiproject.smartcampus.pojo.dto.TeacherUpdateDTO;
import com.aiproject.smartcampus.pojo.po.Course;
import com.aiproject.smartcampus.pojo.po.Teacher;
import com.aiproject.smartcampus.pojo.vo.ChapterQuestionDetailTeacherVO;
import com.aiproject.smartcampus.pojo.vo.ChapterQuestionDetailVO;
import com.aiproject.smartcampus.pojo.vo.ExamStudentVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface TeacherService extends IService<Teacher> {
    Result<TeacherQueryDTO> queryTeachersById(Integer userId);

    Result updateTeacherInfo(Integer userId, TeacherQueryDTO updateDTO);

    Result getAllClassInfo( String couresId);

    Result<List<StudentWrongKnowledgeBO>> getTheMaxUncorrectPoint(String couresId);

    Result<TeacherGetSituationDTO> GetAllSituation(Integer courseId);

    Result<List<Course>> GetAllCourse(Integer teacherId);

    Result<List<TeacherGetStudentDTO>> getStudentInfo(Integer courseId);

    Result getPaper(Integer courseId);

    Result<List<ChapterQuestionDetailTeacherVO>> getHomework(String courseId, String chapterId);

    Result<List<ChapterQuestionDetailVO>> getHomeworkByStudent(String studentId, String courseId, String chapterId);

    Result<List<ExamStudentVO>> getExamStudentInfo(String examId);

    Result releasePaper(String examId);

    Result updateExamStatusById(String examId);

    Result deleteExamById(String examId);

    Result<Integer> getUserIdByteacher(Integer teacherId);

}
