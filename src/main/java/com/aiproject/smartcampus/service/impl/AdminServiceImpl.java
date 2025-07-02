package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.exception.StudentExpection;
import com.aiproject.smartcampus.exception.UserExpection;
import com.aiproject.smartcampus.mapper.AdminMapper;
import com.aiproject.smartcampus.mapper.StudentMapper;
import com.aiproject.smartcampus.mapper.TeacherMapper;
import com.aiproject.smartcampus.mapper.UserMapper;
import com.aiproject.smartcampus.pojo.po.Admin;
import com.aiproject.smartcampus.pojo.po.Student;
import com.aiproject.smartcampus.pojo.po.Teacher;
import com.aiproject.smartcampus.pojo.po.User;
import com.aiproject.smartcampus.service.AdminService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * @program: SmartCampus
 * @description:
 * @author: lk_hhh
 * @create: 2025-07-01 15:11
 **/

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminMapper adminMapper;
    private final TeacherMapper teacherMapper;
    private final StudentMapper studentMapper;
    private final RedisTemplate<String, Student> studentRedisTemplate;
    private final RedisTemplate<String, Teacher> teacherRedisTemplate;
    private final RedisTemplate<String, Admin> adminRedisTemplate;
    private final String StudentIdredisPrefix = "system:student:studentNumber";
    private final String UserIdredisPrefix = "system:user:userId";
    private final String TeacherIdredisPrefix = "system:teacher:teacherNumber";
    private final String AdminIdredisPrefix = "system:admin:adminNumber";
    private final Duration expireTime = Duration.ofDays(3);
    private final UserMapper userMapper;
    private final TransactionTemplate transactionTemplate;
    @Resource(name = "transactionAwareExecutor")
    private Executor executor;
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;


    //学生管理

    /**
     * 根据学号进行查询学生信息
     */
    @Override
    public Result<Student> getStudentBystudentNumber(String studentNumber) {

        String redisKey = StudentIdredisPrefix + studentNumber;
        try {
            // 1. 先从 Redis 获取
            Student student = studentRedisTemplate.opsForValue().get(redisKey);
            if (student != null) {
                log.info("【学生查询成功】来自Redis缓存，studentId: {}", studentNumber);
                studentRedisTemplate.expire(redisKey, expireTime);
                return Result.success(student);
            }

            student = studentMapper.findByStudentNumber(studentNumber);
            if (student == null) {
                log.warn("【学生查询失败】数据库未找到该学号，studentId: {}", studentNumber);
                throw new StudentExpection("未查询到该学号对应的学生信息");
            }

            studentRedisTemplate.opsForValue().set(redisKey, student);
            studentRedisTemplate.expire(redisKey, expireTime);
            log.info("【学生信息已缓存】studentId: {}", studentNumber);

            return Result.success(student);
        } catch (Exception e) {
            log.error("【学生查询异常】studentId: {}, 错误: {}", studentNumber, e.getMessage(), e);
            throw new StudentExpection("根据学号查询学生信息失败，请稍后再试");
        }
    }

    /**
     * 根据学生学号进行删除学生
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result deleteStudentBystudentNumber(String studentNumber) {

        String redisKey = StudentIdredisPrefix + studentNumber;
        try {
            //查询redis
            Student student = studentRedisTemplate.opsForValue().get(redisKey);
            if (student != null) {
                studentRedisTemplate.delete(redisKey);
            }
            //查询数据库
            Student byStudentNumber = studentMapper.findByStudentNumber(studentNumber);
            if (byStudentNumber != null) {
                LambdaQueryWrapper<Student> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(Student::getStudentNumber, byStudentNumber.getStudentNumber());
                int delete = studentMapper.delete(queryWrapper);
                if (delete <= 0) {
                    log.error("删除学号[{}]的学生失败", studentNumber);
                    throw new StudentExpection("删除学生失败");
                }
                LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
                userLambdaQueryWrapper.eq(User::getUserId, byStudentNumber.getUserId());
                int delete1 = userMapper.delete(userLambdaQueryWrapper);
                if (delete1 <= 0) {
                    log.error("删除学号[{}]的学生失败", studentNumber);
                    throw new StudentExpection("删除学生失败");
                }
            }
            log.info("删除学号为[{}]的学生成功", studentNumber);
            return Result.success();

        } catch (Exception e) {
            log.error("【学生删除异常】studentId: {}, 错误: {}", studentNumber, e.getMessage(), e);
            throw new StudentExpection("删除学生信息失败，请稍后再试");
        }

    }

    /**
     * 根据学号进行修改学生信息
     */
    @Override
    public Result updateStudentBystudentNumber(Student student) {

        //获取studentNumber
        String studentNumber = student.getStudentNumber();
        String redisKey = StudentIdredisPrefix + studentNumber;

        //查询用户信息是否存在
        LambdaQueryWrapper<Student> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Student::getStudentNumber, studentNumber);
        Student selectOne = studentMapper.selectOne(queryWrapper);
        if (selectOne == null) {
            log.error("需要修改的学生【{}】不存在", studentNumber);
            throw new StudentExpection("所需修改的学生不存在");
        }

        //异步更新
        executor.execute(() -> {
            studentRedisTemplate.opsForValue().set(redisKey, student);
            studentRedisTemplate.expire(redisKey, expireTime);
            log.info("学生【{}】修改信息已同步到redis中", studentNumber);

            transactionTemplate.execute(status -> {
                try {
                    studentMapper.updateById(student);
                    userMapper.updateById(student.getUser());
                    log.info("学生【{}】修改信息已同步到数据库中", studentNumber);
                    return null;
                } catch (Exception e) {
                    log.error("异步更新学生信息[{}]错误{}", studentNumber, e.getMessage(), e);
                    status.setRollbackOnly();
                    log.error("事务失败：{}", e.getMessage(), e);
                    return null;
                }
            });
        });

        return Result.success();
    }

    /**
     * 新增学生
     */

    @Override
    public Result addStudent(Student student) {

        //判断是否已经存在
        String studentNumber = student.getStudentNumber();
        String redisKey = StudentIdredisPrefix + studentNumber;
        //查询数据库
        LambdaQueryWrapper<Student> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Student::getStudentNumber, studentNumber);
        Student student1 = studentMapper.selectOne(queryWrapper);
        if (student1 != null) {
            log.error("[{}]学生信息已经存在", studentNumber);
            throw new StudentExpection("学生信息已经存在");
        }

        //异步更新
        executor.execute(() -> {

            studentRedisTemplate.opsForValue().set(redisKey, student);
            studentRedisTemplate.expire(redisKey, expireTime);
            log.info("学生【{}】新增信息已同步到redis中", studentNumber);
            //手动开启事务
            transactionTemplate.execute(status -> {
                try {
                    User user = student.getUser();
                    userMapper.insert(user); // 会自动回填 userId
                    student.setUserId(user.getUserId());
                    studentMapper.insert(student);
                    log.info("学生【{}】新增信息已同步到数据库中", studentNumber);
                    return null;
                } catch (Exception e) {
                    log.error("异步新增学生信息[{}]错误{}", studentNumber, e.getMessage(), e);
                    status.setRollbackOnly();
                    log.error("事务失败：{}", e.getMessage(), e);
                    return null;
                }
            });
        });

        return Result.success();
    }

    @Override
    public Result updateUserStatus(String userId, String status) {
        // 查询数据库是否存在相关信息
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUserId, userId);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            log.error("用户【{}】不存在", userId);
            throw new UserExpection("用户不存在");
        }

        // 使用事务同步更新数据库和Redis
        return transactionTemplate.execute(transactionStatus -> {
            try {
                log.info("开始更新用户[{}]状态为[{}]", userId, status);

                // 1. 更新数据库
                LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(User::getUserId, userId);
                updateWrapper.set(User::getStatus, status);
                int updateCount = userMapper.update(null, updateWrapper);

                if (updateCount == 0) {
                    log.error("数据库更新失败，用户[{}]", userId);
                    throw new UserExpection("用户状态更新失败");
                }

                log.info("数据库更新成功，用户[{}]状态已修改为[{}]", userId, status);

                // 2. 重新查询最新数据
                LambdaQueryWrapper<User> freshQueryWrapper = new LambdaQueryWrapper<>();
                freshQueryWrapper.eq(User::getUserId, userId);
                User updatedUser = userMapper.selectOne(freshQueryWrapper);

                // 3. 更新Redis缓存
                updateRedisCache(updatedUser);

                log.info("用户[{}]状态更新完成", userId);
                return Result.success();

            } catch (Exception e) {
                log.error("更新用户[{}]状态失败: {}", userId, e.getMessage(), e);
                transactionStatus.setRollbackOnly();
                throw new UserExpection("用户状态更新失败: " + e.getMessage());
            }
        });
    }

    private void updateRedisCache(User user) {
        try {
            switch (user.getUserType()) {
                case STUDENT: {
                    LambdaQueryWrapper<Student> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(Student::getUserId, user.getUserId());
                    Student student = studentMapper.selectOne(queryWrapper);

                    if (student != null) {
                        student.setUser(user);
                        String key = StudentIdredisPrefix + student.getStudentNumber();
                        studentRedisTemplate.opsForValue().set(key, student);
                        studentRedisTemplate.expire(key, expireTime);
                        log.info("学生[{}]Redis缓存已更新", student.getStudentNumber());
                    }
                    break;
                }

                case TEACHER: {
                    LambdaQueryWrapper<Teacher> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(Teacher::getUserId, user.getUserId());
                    Teacher teacher = teacherMapper.selectOne(queryWrapper);

                    if (teacher != null) {
                        teacher.setUser(user);
                        String key = TeacherIdredisPrefix + teacher.getEmployeeNumber();
                        teacherRedisTemplate.opsForValue().set(key, teacher);
                        teacherRedisTemplate.expire(key, expireTime);
                        log.info("教师[{}]Redis缓存已更新", teacher.getEmployeeNumber());
                    }
                    break;
                }

                case ADMIN: {
                    LambdaQueryWrapper<Admin> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(Admin::getUserId, user.getUserId());
                    Admin admin = adminMapper.selectOne(queryWrapper);

                    if (admin != null) {
                        admin.setUser(user);
                        String key = AdminIdredisPrefix + admin.getAdminNumber();
                        adminRedisTemplate.opsForValue().set(key, admin);
                        adminRedisTemplate.expire(key, expireTime);
                        log.info("管理员[{}]Redis缓存已更新", admin.getAdminNumber());
                    }
                    break;
                }

                default:
                    throw new UserExpection("用户角色不存在");
            }
        } catch (Exception e) {
            log.error("更新Redis缓存失败: {}", e.getMessage(), e);
            // 这里可以选择抛出异常让事务回滚，或者只记录日志继续执行
            // 如果Redis更新失败不影响业务，可以只记录日志
            // 如果需要强一致性，可以抛出异常
            throw new RuntimeException("Redis缓存更新失败", e);
        }
    }


    // 教室端


    /**
     * 根据工号查询教师信息
     * */


    /**
     * 修改用户状态
     * */


}