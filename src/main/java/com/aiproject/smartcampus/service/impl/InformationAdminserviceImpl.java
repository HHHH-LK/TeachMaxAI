package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.commons.utils.UserToTypeUtils;
import com.aiproject.smartcampus.mapper.*;
import com.aiproject.smartcampus.pojo.bo.KnowledgePointErrorSummary;
import com.aiproject.smartcampus.pojo.bo.StudentKnowBO;
import com.aiproject.smartcampus.pojo.bo.StudentWrongKnowledgeBO;
import com.aiproject.smartcampus.pojo.dto.AdminInfoDTO;
import com.aiproject.smartcampus.pojo.dto.AdminUserDTO;
import com.aiproject.smartcampus.pojo.po.*;
import com.aiproject.smartcampus.service.InformationAdminService;
import com.aliyun.core.utils.StringUtils;
import com.aliyun.oss.ServiceException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.lettuce.core.dynamic.annotation.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jaxb.core.v2.TODO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@RequiredArgsConstructor
@Service
public class InformationAdminserviceImpl implements InformationAdminService {
    private final UserMapper userMapper;
    private final UserToTypeUtils userToTypeUtils;
    private final StudentMapper studentMapper;
    private final StudentAnswerMapper studentAnswerMapper;
    private final StudentChapterProgressMapper chapterProgressMapper;
    private final ExamScoreMapper examScoreMapper;
    private final CourseEnrollmentMapper courseEnrollmentMapper;
    private final StudentKnowledgeMasteryMapper studentKnowledgeMasteryMapper;
    private final TeacherMapper teacherMapper;
    private final CourseMapper courseMapper;
    private final AdminMapper adminMapper;
    private final CoursewareResourceMapper coursewareResourceMapper;
    private final KnowledgePointMapper knowledgePointMapper;

//    @Autowired
//    @Qualifier("integerRedisTemplate")
//    private RedisTemplate<String, Integer> redisTemplate;

    private String tokenKey = "token:"; // 默认值

    @Autowired
    private RedisTemplate<String, User> userRedisTemplate;

    @Override
    public Result informationTimes() {
        String[] userTypes = {"student", "teacher"};
        Map<String, Map<String, Integer>> stats = new HashMap<>();

        // 确保 tokenKey 不为 null
        if (tokenKey == null) {
            log.warn("tokenKey 未初始化，使用默认值");
            tokenKey = "token:"; // 设置默认值
        }

        // 获取当前日期和本周日期范围
        final String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<String> weekDates = IntStream.range(0, 7)
                .mapToObj(monday::plusDays)
                .map(date -> date.format(DateTimeFormatter.BASIC_ISO_DATE))
                .collect(Collectors.toList());

        // 获取所有用户登录的 token 键
        String tokenPattern = tokenKey + "*";
        Set<String> allTokenKeys = userRedisTemplate.keys(tokenPattern);

        log.debug("查询模式: {}，找到 {} 个键", tokenPattern, allTokenKeys != null ? allTokenKeys.size() : 0);

        if (allTokenKeys == null || allTokenKeys.isEmpty()) {
            log.warn("未找到任何用户 token 键，使用模式: {}", tokenPattern);
            // 返回空统计结果
            return getEmptyStats(userTypes);
        }

        // 批量获取用户对象
        List<User> users = userRedisTemplate.opsForValue().multiGet(allTokenKeys);

        if (users == null || users.isEmpty()) {
            log.warn("找到 {} 个 token 键，但未获取到用户对象", allTokenKeys.size());
            return getEmptyStats(userTypes);
        }

        log.debug("从 Redis 获取到 {} 个用户对象", users.size());

        // 统计每天每种用户类型的活跃数量
        Map<String, Map<String, Integer>> dateTypeCounts = new HashMap<>();

        for (User user : users) {
            if (user == null || user.getUserType() == null) continue;

            String userType = user.getUserType().toString().toLowerCase();
            LocalDateTime createdAt = user.getCreatedAt();

            // 如果没有创建时间，使用当前时间替代
            if (createdAt == null) {
                log.debug("用户 {} 缺少创建时间，使用当前时间", user.getUserId());
                createdAt = LocalDateTime.now();
            }

            // 格式化为日期字符串 (yyyyMMdd)
            String dateKey = createdAt.format(DateTimeFormatter.BASIC_ISO_DATE).substring(0, 8);

            // 更新统计
            dateTypeCounts
                    .computeIfAbsent(dateKey, k -> new HashMap<>())
                    .merge(userType, 1, Integer::sum);
        }

        log.debug("用户登录统计: {}", dateTypeCounts);

        // 计算每个用户类型的今日和本周统计
        for (String userType : userTypes) {
            // 当日统计
            int todayCount = dateTypeCounts.getOrDefault(today, Collections.emptyMap())
                    .getOrDefault(userType, 0);

            // 本周统计 (包含今日)
            int weekCount = weekDates.stream()
                    .mapToInt(date -> dateTypeCounts.getOrDefault(date, Collections.emptyMap())
                            .getOrDefault(userType, 0))
                    .sum();

            Map<String, Integer> typeStats = new HashMap<>();
            typeStats.put("today", todayCount);
            typeStats.put("week", weekCount);
            stats.put(userType, typeStats);
        }

        log.info("最终统计结果: {}", stats);
        return Result.success(stats);
    }

    // 辅助方法：返回空统计结果
    private Result getEmptyStats(String[] userTypes) {
        Map<String, Map<String, Integer>> stats = new HashMap<>();
        for (String type : userTypes) {
            Map<String, Integer> typeStats = new HashMap<>();
            typeStats.put("today", 0);
            typeStats.put("week", 0);
            stats.put(type, typeStats);
        }
        return Result.success(stats);
    }

    @Override
    public Result deleteInformation(String userId) {
        try {
            // 1. 验证用户ID格式
            int userIntId;
            try {
                userIntId = Integer.parseInt(userId);
            } catch (NumberFormatException e) {
                return Result.error("无效的用户ID格式");
            }

            // 2. 验证用户是否存在
            User user = userMapper.selectById(userIntId);
            if (user == null) {
                return Result.error("用户不存在");
            }

            // 3. 根据用户类型执行不同的删除操作
            User.UserType userType = user.getUserType();

            switch (userType) {
                case STUDENT:
                    return deleteStudentInformation(user);
                case TEACHER:
                    return deleteTeacherInformation(user);
                case ADMIN:
                    return deleteAdminInformation(user);
                default:
                    return Result.error("未知的用户类型: " + userType.name());
            }
        } catch (Exception e) {
            log.error("删除用户信息失败: {}", e.getMessage(), e);
            return Result.error("服务器内部错误: " + e.getMessage());
        }
    }

    @Override
    public Result getAllResources() {
        // 获取所有课程资源
        List<CoursewareResource> resources = coursewareResourceMapper.selectList(null);
        if (resources.isEmpty()) {
            return Result.error("没有找到任何资源");
        }
        return Result.success(resources);
    }

    @Override
    public Result deleteResource(String resourceId) {
        try {
            // 验证资源ID格式
            int resourceIntId;
            try {
                resourceIntId = Integer.parseInt(resourceId);
            } catch (NumberFormatException e) {
                return Result.error("无效的资源ID格式");
            }

            // 查询资源是否存在
            CoursewareResource resource = coursewareResourceMapper.selectById(resourceIntId);
            if (resource == null) {
                return Result.error("资源不存在");
            }

            // 删除资源
            int result = coursewareResourceMapper.deleteById(resourceIntId);
            if (result > 0) {
                return Result.success("资源删除成功");
            } else {
                return Result.error("资源删除失败");
            }
        } catch (Exception e) {
            log.error("删除资源失败: {}", e.getMessage(), e);
            return Result.error("服务器内部错误: " + e.getMessage());
        }
    }

    @Override
    public Result getAdminInfo() {
        // 获取当前登录用户的ID
//        String userId = userToTypeUtils.change();
        String userId = "1";

        // 查询用户信息
        User user = userMapper.selectById(userId);

        if (user == null) {
            return Result.error("管理员信息不存在");
        }
        Admin admin = adminMapper.selectById(userId);

        // 返回用户和管理员信息，可以使用 Map 或自定义对象封装
        Map<String, Object> info = new HashMap<>();
        info.put("user", user);
        info.put("admin", admin);
        return Result.success(info);
    }

    @Override
    public Result updateAdminInfo(AdminInfoDTO adminInfo) {
        try {
            // 1. 获取当前登录用户ID (需替换实际获取逻辑)
            // String currentUserId = userToTypeUtils.change();
            String currentUserId = "1";
            int userId = Integer.parseInt(currentUserId);

            // 2. 数据合法性校验
            if (adminInfo == null) {
                return Result.error("更新信息不能为空");
            }
            if (StringUtils.isBlank(adminInfo.getAdminNumber())) {
                return Result.error("管理员工号不能为空");
            }

            // 3. 验证管理员级别有效性
            List<String> validLevels = Arrays.asList("super_admin", "admin", "operator");
            if (!validLevels.contains(adminInfo.getAdminLevel())) {
                return Result.error("无效的管理员级别");
            }

            // 4. 检查工号冲突 (排除当前管理员自身)
            Integer existingId = adminMapper.checkAdminNumberConflict(
                    adminInfo.getAdminNumber(),
                    userId
            );
            if (existingId != null && !existingId.equals(userId)) {
                return Result.error("该工号已被其他管理员使用");
            }

            // 5. 构建用户实体并更新 users 表
            User user = new User();
            user.setUserId(userId);
            user.setRealName(adminInfo.getAdminName());
            user.setEmail(adminInfo.getAdminEmail());
            user.setPhone(adminInfo.getAdminPhone());
            userMapper.updateById(user);

            // 6. 构建管理员实体并更新 admins 表
            Admin admin = new Admin();
            admin.setUserId(userId);
            admin.setAdminNumber(adminInfo.getAdminNumber());
            admin.setDepartment(adminInfo.getAdminDepartment());
            admin.setPosition(adminInfo.getAdminRole()); // 职位对应角色
            admin.setAdminLevel(Admin.AdminLevel.fromValue(adminInfo.getAdminLevel()));
            adminMapper.updateByUserId(admin);

            // 7. 返回成功响应
            return Result.success("管理员信息更新成功");
        } catch (NumberFormatException e) {
            throw new ServiceException("用户ID格式错误");
        } catch (DuplicateKeyException e) {
            throw new ServiceException("数据唯一性冲突，请检查工号");
        } catch (Exception e) {
            // 记录详细日志
            log.error("管理员信息更新失败: {}", e.getMessage());
            throw new ServiceException("系统内部错误，更新操作失败");
        }
    }

    @Override
    public Result updateUserInfo(AdminUserDTO dto) {
        try {
            // 1. 数据校验
            if (dto == null) {
                return Result.error("更新信息不能为空");
            }
            if (dto.getUserId() == null) {
                return Result.error("用户ID不能为空");
            }

            // 获取原用户信息
            User originalUser = userMapper.selectById(dto.getUserId());
            if (originalUser == null) {
                return Result.error("用户不存在");
            }

            // 2. 准备用户实体更新
            User user = new User();
            user.setUserId(dto.getUserId());
            user.setRealName(dto.getRealName());
            user.setEmail(dto.getEmail());

            // 3. 用户类型变更处理
            String originalType = String.valueOf(originalUser.getUserType()).toLowerCase();
            System.out.println("Original User Type: " + originalType);
            String newType = dto.getUserType();
            boolean typeChanged = !originalType.equals(newType);

            // 3.1 如果用户类型变更
            if (typeChanged) {
                // 检查并处理外键依赖（针对教师类型）
                if ("teacher".equals(originalType)) {
                    Result dependencyResult = handleTeacherDependencies(dto.getUserId());
                    if (!dependencyResult.isSuccess()) {
                        return dependencyResult; // 存在关联数据，返回错误结果
                    }
                }

                // 删除原类型对应的扩展表记录
                switch (originalType) {
                    case "admin":
                        adminMapper.deleteByUserId(dto.getUserId());
                        break;
                    case "student":
                        studentMapper.deleteByUserId(dto.getUserId());
                        break;
                    case "teacher":
                        teacherMapper.deleteByUserId(dto.getUserId());
                        break;
                }

                // 在新扩展表中创建记录
                switch (newType) {
                    case "admin":
                        Admin admin = createDefaultAdmin(dto.getUserId());
                        adminMapper.insert(admin);
                        break;
                    case "student":
                        Student student = createDefaultStudent(dto.getUserId());
                        studentMapper.insert(student);
                        break;
                    case "teacher":
                        Teacher teacher = createDefaultTeacher(dto.getUserId());
                        teacherMapper.insert(teacher);
                        break;
                    default:
                        return Result.error("不支持的用户类型");
                }

                // 更新主表用户类型
                user.setUserType(User.UserType.fromValue(newType));
            }

            // 4. 更新用户主表
            userMapper.updateById(user);

            return Result.success("用户信息更新成功");

        } catch (DuplicateKeyException e) {
            return Result.error("唯一键冲突，请检查数据");
        } catch (Exception e) {
            log.error("用户信息更新失败: {}", e.getMessage());
            return Result.error("系统内部错误，操作失败");
        }
    }

    @Override
    public Result getHighFrequencyErrorPoints() {
        try {
            // 1. 获取所有学生的答题数据（包括正确和错误）
            List<StudentKnowBO> allAnswers = studentAnswerMapper.getAllAnswerKnowledgeFrequency();

            // 2. 按知识点分组并统计错误频率
            Map<Integer, KnowledgePointErrorSummary> summaryMap = new HashMap<>();

            for (StudentKnowBO answer : allAnswers) {
                int pointId = answer.getPointId();

                KnowledgePointErrorSummary summary = summaryMap.computeIfAbsent(pointId, k ->
                        new KnowledgePointErrorSummary(
                                pointId,
                                answer.getPointName(),
                                answer.getDescription(),
                                answer.getDifficultyLevel(),
                                answer.getKeywords(),
                                answer.getCourseName()
                        )
                );

                // 更新统计信息
                if (answer.getIsCorrect() != null && !answer.getIsCorrect()) {
                    summary.incrementWrongAnswerCount(1);
                }
                summary.incrementTotalAnswerCount(1);
            }

            // 3. 转换为列表并排序（按错误频率降序）
            List<KnowledgePointErrorSummary> resultList = new ArrayList<>(summaryMap.values());
            resultList.sort((a, b) -> b.getTotalWrongCount() - a.getTotalWrongCount());

            // 4. 计算平均错误率
            resultList.forEach(summary -> {
                if (summary.getTotalAnswerCount() > 0) {
                    double errorRate = (double) summary.getTotalWrongCount() / summary.getTotalAnswerCount() * 100;
                    summary.setAverageErrorRate(Math.round(errorRate * 100.0) / 100.0);
                } else {
                    summary.setAverageErrorRate(0.0); // 避免除零错误
                }
            });

            return Result.success(resultList);

        } catch (Exception e) {
            log.error("获取高频错误知识点失败: {}", e.getMessage());
            return Result.error("获取高频错误知识点失败");
        }
    }

    // 处理教师外键依赖并返回结果
    private Result handleTeacherDependencies(Integer userId) {
        try {
            // 获取教师ID
            Teacher teacher = teacherMapper.selectByUserId(userId);
            if (teacher == null) {
                return Result.success(); // 没有教师记录，无需处理
            }

            // 1. 查询该教师关联的课件资源
            List<Course> resources = courseMapper.findByTeacherId(teacher.getTeacherId());

            if (!resources.isEmpty()) {
                // 存在关联数据，返回错误结果
                return Result.error("该教师存在关联的课件资源，无法变更用户类型");
            }

            return Result.success();

        } catch (Exception e) {
            log.error("处理教师依赖失败: {}", e.getMessage());
            return Result.error("处理教师关联数据失败");
        }
    }

    // 创建默认管理员记录 - 生成唯一临时值
    private Admin createDefaultAdmin(Integer userId) {
        LocalDateTime now = LocalDateTime.now();
        // 生成唯一临时工号，避免唯一键冲突
        String uniqueSuffix = String.format("%d_%d", userId, System.currentTimeMillis() % 1000);

        return new Admin()
                .setUserId(userId)
                .setAdminNumber("ADMIN_" + uniqueSuffix)  // 唯一临时工号
                .setDepartment("未指定部门")
                .setPosition("管理员")
                .setAdminLevel(Admin.AdminLevel.fromValue("operator"))  // 默认设置为操作员
                .setPermissions("[]")
                .setStatus(Admin.AdminStatus.fromValue("active"))
                .setCreatedAt(now)
                .setUpdatedAt(now);
    }

    // 创建默认学生记录 - 生成唯一临时值
    private Student createDefaultStudent(Integer userId) {
        // 生成唯一临时学号，避免唯一键冲突
        String uniqueSuffix = String.format("%d_%d", userId, System.currentTimeMillis() % 1000);

        return new Student()
                .setUserId(userId)
                .setStudentNumber("STU_" + uniqueSuffix)  // 唯一临时学号
                .setGrade("未指定年级")
                .setClassName("未指定班级");
    }

    // 创建默认教师记录 - 生成唯一临时值
    private Teacher createDefaultTeacher(Integer userId) {
        // 生成唯一临时工号，避免唯一键冲突
        String uniqueSuffix = String.format("%d_%d", userId, System.currentTimeMillis() % 1000);

        return new Teacher()
                .setUserId(userId)
                .setEmployeeNumber("TEA_" + uniqueSuffix)  // 唯一临时工号
                .setDepartment("未指定部门");
    }

    // 删除学生信息
    private Result deleteStudentInformation(User user) {
        // 获取学生信息
        Student student = studentMapper.selectOne(
                new QueryWrapper<Student>().eq("user_id", user.getUserId())
        );
        if (student == null) {
            return Result.error("该用户没有学生信息");
        }
        int studentId = student.getStudentId();

        // 删除学生关联数据
        deleteStudentRelatedData(studentId);
        // 删除学生属性
        studentMapper.deleteById(studentId);
        // 删除用户信息
        userMapper.deleteById(user.getUserId());
        return Result.success("学生信息删除成功");
    }

    // 删除教师信息
    private Result deleteTeacherInformation(User user) {
        // 获取教师信息
        Teacher teacher = teacherMapper.selectOne(
                new QueryWrapper<Teacher>().eq("user_id", user.getUserId())
        );
        if (teacher == null) {
            return Result.error("该用户没有教师信息");
        }
        int teacherId = teacher.getTeacherId();

//            // 删除教师关联数据
        deleteTeacherRelatedData(user);
        // 删除教师属性
        teacherMapper.deleteById(teacherId);
        // 删除用户信息
        userMapper.deleteById(user.getUserId());

        return Result.success("教师信息删除成功");
    }

    // 删除教师相关数据
    private void deleteTeacherRelatedData(User user) {
        // 获取教师ID
        Teacher teacher = teacherMapper.selectOne(
                new QueryWrapper<Teacher>().eq("user_id", user.getUserId())
        );
        if (teacher == null) return;
        int teacherId = teacher.getTeacherId();

        // 1. 获取教师创建的所有课程ID
        List<Course> courses = courseMapper.selectList(
                new QueryWrapper<Course>().eq("teacher_id", teacherId)
        );
        List<Integer> courseIds = courses.stream()
                .map(Course::getCourseId)
                .collect(Collectors.toList());
        // 2. 删除课程关联的课件资源
        if (!courseIds.isEmpty()) {
            coursewareResourceMapper.delete(
                    new QueryWrapper<CoursewareResource>().in("course_id", courseIds)
            );
        }

        // 3. 删除课程本身
        if (!courseIds.isEmpty()) {
            courseMapper.deleteBatchIds(courseIds);
        }

        // 4. 删除教师创建的其他课件资源（不关联课程的）
        coursewareResourceMapper.delete(
                new QueryWrapper<CoursewareResource>().eq("teacher_id", teacherId)
        );
    }

    // 删除管理员信息
    private Result deleteAdminInformation(User user) {
        int result = userMapper.deleteById(user.getUserId());
        return result > 0 ? Result.success("管理员信息删除成功") : Result.error("删除管理员信息失败");
    }

    // 删除学生关联数据
    private void deleteStudentRelatedData(int studentId) {
        studentAnswerMapper.delete(
                new QueryWrapper<StudentAnswer>().eq("student_id", studentId)
        );

        studentKnowledgeMasteryMapper.delete(
                new QueryWrapper<StudentKnowledgeMastery>().eq("student_id", studentId)
        );

        chapterProgressMapper.delete(
                new QueryWrapper<StudentChapterProgress>().eq("student_id", studentId)
        );

        courseEnrollmentMapper.delete(
                new QueryWrapper<CourseEnrollment>().eq("student_id", studentId)
        );

        examScoreMapper.delete(
                new QueryWrapper<ExamScore>().eq("student_id", studentId)
        );

    }
}
