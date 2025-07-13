package com.aiproject.smartcampus.service;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.dto.ChapterTestCreateDTO;
import com.aiproject.smartcampus.pojo.dto.StudentAnswerDTO;
import com.aiproject.smartcampus.pojo.dto.StudentStudyDTO;
import com.aiproject.smartcampus.pojo.dto.StudentTextAnswerDTO;
import com.aiproject.smartcampus.pojo.vo.*;

import java.util.List;

/**
 * @program: ss
 * @description: 章节处理
 * @author: lk_hhh
 * @create: 2025-07-04 15:53
 **/
public interface ChapterService {
    Result<List<CourseChapterVO>> selectChapterByCrouseId(String courseId);

    Result<List<ChapterKnowledgePointVO>> selectAllKnowledgeByChaptId(String chaptId);

    Result<KnowledgePointMaterialSimpleSpliderVO> selectAllMaterialBypointId(String chapterId,String courseId);

    Result<MaterialDetailSeparatedVO> selectMaterialByMaterialId(String materialId);

    Result<List<ChapterQuestionDetailVO>> getAllTextByChapterId(String chapterId,String courseId);

    Result startStudy(StudentStudyDTO studentStudyDTO);

    Result endStudy(StudentStudyDTO studentStudyDTO);

    Result finsh(StudentStudyDTO studentStudyDTO);

    Result getlearningprogress(String chapterId, String courseId);

    Result getAllUncorrectTest(String courseId);

    Result juTest(StudentTextAnswerDTO studentTextAnswerDTO);

    Result setStudentAwser(StudentTextAnswerDTO studentTextAnswerDTO);

}