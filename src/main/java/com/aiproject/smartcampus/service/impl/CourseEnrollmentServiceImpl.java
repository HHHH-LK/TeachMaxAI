package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.commons.utils.UserToTypeUtils;
import com.aiproject.smartcampus.mapper.CourseEnrollmentMapper;
import com.aiproject.smartcampus.mapper.CourseMapper;
import com.aiproject.smartcampus.pojo.po.Course;
import com.aiproject.smartcampus.pojo.po.CourseEnrollment;
import com.aiproject.smartcampus.service.CourseEnrollmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

import static com.aiproject.smartcampus.contest.CourseContest.HAVING_ADD_COURSE;
import static com.aiproject.smartcampus.contest.CourseContest.NO_EXIST_COURSE;

@Service
@Slf4j
@RequiredArgsConstructor
public class CourseEnrollmentServiceImpl extends ServiceImpl<CourseEnrollmentMapper, CourseEnrollment> implements CourseEnrollmentService {

    private final UserToTypeUtils userToTypeUtils;
    private final CourseMapper courseMapper;
    private final CourseEnrollmentMapper courseEnrollmentMapper;
    private static final ExecutorService ADD_COURSEENROLLMENTS_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final BlockingQueue<CourseEnrollment> queue= new LinkedBlockingQueue<>();

    //初始化线程,启动线程任务
    @PostConstruct
    public void init(){
        ADD_COURSEENROLLMENTS_EXECUTOR.submit(
                () -> {
                    //线程是否处于中断状态
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            CourseEnrollment courseEnrollment = queue.take();
                            courseEnrollmentMapper.insert(courseEnrollment);
                            log.info("添加选课信息成功，学生id：{},课程id：{}", courseEnrollment.getStudentId(), courseEnrollment.getCourseId());
                        }
                        catch(InterruptedException e){
                            log.error("添加选课信息线程被中断",e);
                            Thread.currentThread().interrupt();
                            break;
                        }
                        catch(Exception e){
                            e.printStackTrace();
                            log.error("添加选课信息线程出错",e);
                        }
                    }
                }
        );
    }

    //销毁线程
    @PreDestroy
    public void destroy(){
        ADD_COURSEENROLLMENTS_EXECUTOR.shutdown();
        log.info("开始关闭添加选课信息线程池");
        try{
            if(!ADD_COURSEENROLLMENTS_EXECUTOR.awaitTermination(30, TimeUnit.SECONDS))
            {
                log.error("关闭添加选课信息线程失败，尝试强制关闭");
                List<Runnable> runnables = ADD_COURSEENROLLMENTS_EXECUTOR.shutdownNow();
                log.error("强制关闭线程数量："+runnables.size());
                //再次等待线程关闭
                if(!ADD_COURSEENROLLMENTS_EXECUTOR.awaitTermination(30, TimeUnit.SECONDS))
                {
                    log.error("关闭添加选课信息线程失败，请管理员进行检查");
                }
            }
        }catch(InterruptedException e){
            log.error("关闭添加选课信息线程失败",e);
            ADD_COURSEENROLLMENTS_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }finally {
            //清空队列
            queue.clear();
            log.info("添加选课信息线程关闭成功");
        }
    }


    /**
     * 添加选课信息
     * @param courseId
     * @return
     * */
    @Override
    public Result<String> addCourseEnrollment(Integer courseId) {
        String studentId = userToTypeUtils.change();
        //校验课程是否存在
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            return Result.error(NO_EXIST_COURSE);
        }
        //校验学生是否已经选课
        LambdaQueryWrapper<CourseEnrollment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(studentId!= null, CourseEnrollment::getStudentId, studentId);
        List<CourseEnrollment> courseEnrollments = courseEnrollmentMapper.selectList(wrapper);
        //判断是否选过课
        for (CourseEnrollment enrollment : courseEnrollments) {
            if (enrollment.getCourseId().equals(courseId)) {
                return Result.error(HAVING_ADD_COURSE);
            }
        }

        //异步处理选课信息
        CourseEnrollment courseEnrollment = new CourseEnrollment()
                .setStudentId(Integer.valueOf(studentId))
                .setCourseId(courseId)
                .setEnrollmentDate(LocalDateTime.now());
        //将选课信息放入阻塞队列
        try {
            queue.put(courseEnrollment);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return Result.success("选课成功");
    }

    @Override
    public Result<String> exitCourse(Integer courseId) {
        String studentId = userToTypeUtils.change();

        // 校验课程是否存在
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            return Result.error("课程不存在");
        }

        // 直接查询学生是否选了这门课
        LambdaQueryWrapper<CourseEnrollment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseEnrollment::getStudentId, studentId)
                .eq(CourseEnrollment::getCourseId, courseId);
        CourseEnrollment enrollment = courseEnrollmentMapper.selectOne(wrapper);

        // 没有选课记录
        if (enrollment == null) {
            return Result.error("您未选择该课程，无法退选");
        }

        try {
            // 直接删除选课记录
            int result = courseEnrollmentMapper.deleteBycourseId(courseId);
            if (result > 0) {
                // 异步记录成功日志
                ADD_COURSEENROLLMENTS_EXECUTOR.submit(() -> {
                    log.info("退课成功，学生ID：{}, 课程ID：{}", studentId, courseId);
                });
                return Result.success("退课成功");
            } else {
                return Result.error("退课操作失败，请稍后重试");
            }
        } catch (Exception e) {
            log.error("退课发生异常, 学生ID: {}, 课程ID: {}", studentId, courseId, e);
            return Result.error("系统错误，退课失败");
        }
    }
}
