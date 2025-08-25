/**
 * 用户配置文件
 * 包含用户ID、学生ID和课程ID等配置信息
 * 到时候通过接口或者持久化进行获取
 */
import {getCurrentUserId} from "@/utils/userUtils.js";
import {studentService} from "@/services/api.js";

// 先初始化用户配置（studentId暂时为null）
export const userConfig = {
  // 用户ID
  userId: 16,

  // 学生ID（需要异步获取）
  studentId: 1,

  // 课程ID列表 (1-4)
  courseIds: [19, 20],

  // 课程信息映射
  courseInfo: {
    1: { name: '课程1', description: '第一门课程' },
    2: { name: '课程2', description: '第二门课程' },
    3: { name: '课程3', description: '第三门课程' },
    4: { name: '课程4', description: '第四门课程' },
    5: { name: '课程5', description: '第五门课程' },
    6: { name: '课程6', description: '第六门课程' },
    7: { name: '课程7', description: '第七门课程' },
    8: { name: '课程8', description: '第八门课程' }
  },

  // 初始化方法，用于异步获取studentId
  async init() {
    try {
      const response = await studentService.getStudentIdByUserId(this.userId);
      this.studentId = response.data; // 假设返回的数据在data字段中
    } catch (error) {
      console.error('获取学生ID失败:', error);
    }
  },

  // 获取所有课程ID
  getAllCourseIds() {
    return this.courseIds;
  },

  // 根据课程ID获取课程信息
  getCourseInfo(courseId) {
    return this.courseInfo[courseId] || null;
  },

  // 检查课程ID是否有效
  isValidCourseId(courseId) {
    return this.courseIds.includes(courseId);
  }
};

// 导出默认配置
export default userConfig;
