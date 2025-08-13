package com.aiproject.smartcampus.model.functioncalling;

import com.aiproject.smartcampus.mapper.CourseMapper;
import com.aiproject.smartcampus.mapper.TeacherMapper;
import com.aiproject.smartcampus.model.functioncalling.toolutils.CreateTeacherAssignToolUtils;
import com.aiproject.smartcampus.pojo.po.Course;
import com.aiproject.smartcampus.pojo.po.Teacher;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Random;

/**
 * 教师分配工具 - 智能分配教师到课程
 */
@Data
@Slf4j
@Component
@RequiredArgsConstructor
public class TeacherAssignTool implements Tool {

    private final CourseMapper courseMapper;
    private final TeacherMapper teacherMapper;
    private final CreateTeacherAssignToolUtils createTeacherAssignToolUtils;

    // 课程ID
    private Integer courseId;

    // 分配结果
    private String result;

    @Override
    @Transactional
    public void run() {
        log.info("正在为课程[{}]智能分配教师", courseId);

        // 参数校验
        if (!validateParameters()) {
            return;
        }

        try {
            // 获取课程信息
            Course course = courseMapper.selectById(courseId);
            if (course == null) {
                log.error("课程不存在: {}", courseId);
                result = "课程不存在";
                return;
            }

            // 记录原教师ID（如果有）
            Integer originalTeacherId = course.getTeacherId();

            // 智能分配教师
            assignTeacher(course);

            // 记录分配结果
            log.info("教师分配成功: 课程[{}]原教师ID: {}, 新教师ID: {}",
                    courseId,
                    originalTeacherId,
                    course.getTeacherId());

            result = "教师分配成功 - 原教师ID: " + originalTeacherId + ", 新教师ID: " + course.getTeacherId();

        } catch (Exception e) {
            log.error("教师分配失败", e);
            result = "教师分配失败: " + e.getMessage();
        }
    }

    /**
     * 智能分配教师
     */
    private void assignTeacher(Course course) {
        // 记录原教师ID（如果有）
        Integer originalTeacherId = course.getTeacherId();

        // 使用AI提取学科领域
        String subjectArea = createTeacherAssignToolUtils.extractSubjectArea(course.getCourseName());

        if (StringUtils.hasText(subjectArea)) {
            try {
                // 1. 查找匹配部门的教师
                List<Teacher> matchedTeachers = teacherMapper.findByDepartment(subjectArea);

                if (matchedTeachers.isEmpty()) {
                    // 2. 如果没有完全匹配的教师，查找相关领域的教师
                    matchedTeachers = teacherMapper.findByRelatedDepartment(subjectArea);
                }

                if (matchedTeachers.isEmpty()) {
                    // 3. 如果还是没有匹配的教师，使用AI推荐最适合的教师
                    log.info("没有匹配的教师，使用AI推荐最适合的教师");
                    matchedTeachers = teacherMapper.findAll();
                    Teacher recommendedTeacher = createTeacherAssignToolUtils.recommendBestTeacher(
                            course.getCourseName(), matchedTeachers);

                    if (recommendedTeacher != null) {
                        course.setTeacherId(recommendedTeacher.getTeacherId());
                        courseMapper.updateById(course);
                        log.info("AI推荐教师: {} (ID: {})",
                                recommendedTeacher.getEmployeeNumber(),
                                recommendedTeacher.getTeacherId());
                        return;
                    }

                    // 如果AI推荐失败，使用随机分配
                    log.warn("AI推荐教师失败，使用随机分配");
                    assignRandomTeacher(course);
                    return;
                }

                // 4. 随机选择一个教师
                Random random = new Random();
                Teacher selectedTeacher = matchedTeachers.get(random.nextInt(matchedTeachers.size()));
                course.setTeacherId(selectedTeacher.getTeacherId());

                // 更新课程信息
                courseMapper.updateById(course);

                log.info("为课程[{}]智能分配教师: {} (ID: {})",
                        course.getCourseName(),
                        selectedTeacher.getEmployeeNumber(),
                        selectedTeacher.getTeacherId());
            } catch (Exception e) {
                log.warn("智能分配教师失败: {}", e.getMessage());
                throw new RuntimeException("智能分配教师失败: " + e.getMessage());
            }
        } else {
            log.warn("无法从课程名称中提取学科领域，使用AI推荐最适合的教师");
            recommendBestTeacher(course);
        }
    }

    /**
     * 使用AI推荐最适合的教师
     */
    private void recommendBestTeacher(Course course) {
        try {
            List<Teacher> allTeachers = teacherMapper.findAll();

            if (allTeachers.isEmpty()) {
                log.error("没有可用的教师可以分配");
                throw new RuntimeException("没有可用的教师可以分配");
            }

            // 使用AI推荐最适合的教师
            Teacher recommendedTeacher = createTeacherAssignToolUtils.recommendBestTeacher(
                    course.getCourseName(), allTeachers);

            if (recommendedTeacher != null) {
                course.setTeacherId(recommendedTeacher.getTeacherId());
                courseMapper.updateById(course);
                log.info("AI推荐教师: {} (ID: {})",
                        recommendedTeacher.getEmployeeNumber(),
                        recommendedTeacher.getTeacherId());
                return;
            }

            // 如果AI推荐失败，使用随机分配
            log.warn("AI推荐教师失败，使用随机分配");
            assignRandomTeacher(course);

        } catch (Exception e) {
            log.warn("教师推荐失败: {}", e.getMessage());
            throw new RuntimeException("教师推荐失败: " + e.getMessage());
        }
    }

    /**
     * 随机分配教师
     */
    private void assignRandomTeacher(Course course) {
        try {
            List<Teacher> allTeachers = teacherMapper.findAll();

            if (allTeachers.isEmpty()) {
                log.error("没有可用的教师可以分配");
                throw new RuntimeException("没有可用的教师可以分配");
            }

            // 随机选择一个教师
            Random random = new Random();
            Teacher selectedTeacher = allTeachers.get(random.nextInt(allTeachers.size()));
            course.setTeacherId(selectedTeacher.getTeacherId());

            // 更新课程信息
            courseMapper.updateById(course);

            log.info("为课程[{}]随机分配教师: {} (ID: {})",
                    course.getCourseName(),
                    selectedTeacher.getEmployeeNumber(),
                    selectedTeacher.getTeacherId());
        } catch (Exception e) {
            log.warn("随机分配教师失败: {}", e.getMessage());
            throw new RuntimeException("随机分配教师失败: " + e.getMessage());
        }
    }

    /**
     * 参数校验
     */
    private boolean validateParameters() {
        if (courseId == null) {
            log.error("课程ID不能为空");
            result = "课程ID不能为空";
            return false;
        }
        return true;
    }
}