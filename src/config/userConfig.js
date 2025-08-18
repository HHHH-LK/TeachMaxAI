/**
 * 用户配置文件
 * 包含用户ID、学生ID和课程ID等配置信息
 * 到时候通过接口或者持久化进行获取
 */

export const userConfig = {
  // 用户ID
  userId: 17,
  
  // 学生ID
  studentId: 2,
  
  // 课程ID列表 (1-4)
  courseIds: [1, 2, 3, 4],
  
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
