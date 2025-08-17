import apiClient from "@/utils/https";

// 文件上传-post
export const fileService = {
  uploadFile: async (file, onProgress) => {
    // 文件类型校验
    // const allowedTypes = ['image/png', 'image/jpeg', 'application/pdf'];
    // if (!allowedTypes.includes(file.type)) {
    //   throw new Error('仅支持PNG、JPEG和PDF格式');
    // }

    // 文件大小限制（5MB）
    // const maxSize = 5 * 1024 * 1024;
    // if (file.size > maxSize) {
    //   throw new Error('文件大小不能超过5MB');
    // }

    const formData = new FormData();
    formData.append('file', file);

    return await apiClient.post('/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress: progressEvent => {
        const percentCompleted = Math.round(
            (progressEvent.loaded * 100) / progressEvent.total
        );
        onProgress?.(percentCompleted);
      }
    });
  }
};

// 教师服务 - 备课与设计、考核内容生成、学情数据分析
export const teacherService = {
  // 获取教师个人信息
  getTeacherInfo: async (userId) => {
    return await apiClient.get(`/teacher/query/${userId}`);
  },

  /**
   * 根据课程ID获取章节信息
   * @param {string} courseId - 课程ID
   * @returns {Promise<object>} 章节信息列表
   */
  getChapterByCourseId: async (courseId) => {
    return await apiClient.get('/chapter/getChapter', {
      params: { courseId }
    });
  },

  /**
   * 根据章节ID查询所有知识点信息
   * @param {string} chaptId - 章节ID
   * @returns {Promise<object>} 知识点信息列表
   */
  getKnowledgePointsByChapterId: async (chaptId) => {
    return await apiClient.get('/chapter/selectAllKnowledgeBychaptId', {
      params: { chaptId }
    });
  },

  // AI学情分析
  generateLearningAnalysis: async (params) => {
    return await apiClient.post('/teacher/ai/learning-analysis', {
      studentId: params.studentId,
      courseId: params.courseId,
      classId: params.classId,
      analysisType: params.analysisType || 'comprehensive'
    });
  },

  // 获取班级学生列表
  getClassStudents: async (courseId) => {
    return await apiClient.get(`/teacher/getStudentInfo/${courseId}`);
  },

  // 获取班级考试统计
  getClassExamStats: async (classId) => {
    return await apiClient.get(`/teacher/classes/${classId}/exam-stats`);
  },

  // 获取学生考试记录
  getStudentExamHistory: async (studentId) => {
    return await apiClient.get(`/teacher/students/${studentId}/exam-history`);
  },

  // 学情数据分析 - 对学生提交的答案进行自动化检测，提供错误定位与修正建议
  analyzeStudentAnswer: async (answerId) => {
    return await apiClient.post(`/teacher/analyze-answer/${answerId}`);
  },

  // 获取班级学情数据分析
  getClassAnalysis: (courseId) => {
    return apiClient.post('/teacher/ai/aiclassAiayaisc', null, {
      params: { courseId } // 放在配置对象的params中
    });
  },

  /**
   * 查询作业
   * @param {string} courseId - 课程ID
   * @param {string} chapterId - 章节ID
   * @returns {Promise<object>} 作业信息
   */
  getHomework: async (courseId, chapterId) => {
    return await apiClient.get('/teacher/getHomework', {
      params: { courseId, chapterId }
    });
  },

  /**
   * 根据学生获取所有作业信息
   * @param {string} studentId - 学生ID
   * @param {string} courseId - 课程ID
   * @param {string} chapterId - 章节ID
   * @returns {Promise<object>} 作业信息列表
   */
  getHomeworkByStudent: async (studentId, courseId, chapterId) => {
    return await apiClient.get('/teacher/getHomeworkByStudent', {
      params: { studentId, courseId, chapterId }
    });
  },

  // 获取学生个人学情数据分析
  getStudentAnalysis: async (studentId, courseId) => {
    return await apiClient.get('/teacher/student-analysis', {
      params: { studentId, courseId }
    });
  },

  // 获取知识点掌握情况分析
  getKnowledgePointAnalysis: async (courseId, classId) => {
    return await apiClient.get('/teacher/knowledge-point-analysis', {
      params: { courseId, classId }
    });
  },

  // 教案生成 - 将所有备课资料字段拼成一段话传递给后端
  generateLessonPlan: async (content, courseId, chapterId) => {
    return await apiClient.get('/teacher/ai/TeacherTextCreate', {
      params:{
        content: content,
        courseId: courseId,
        chapterId: chapterId
      }
    });
  },

  // 获取所有课程
  getAllCourse: async (teacherId) =>{
    return apiClient.get(`/teacher/getClass/${teacherId}`)
  },

  //获取课程下所有学生
  getAllStudent: async(courseId)=> {
    return apiClient.get(`/teacher/getStudentInfo/${courseId}`)
  },

  // 获取学生信息（根据学号）
  getStudentByNumber: async (studentNumber) => {
    return await apiClient.get(`/stu/student/${studentNumber}`);
  },

  // 考核管理相关接口
  assessment: {
    //获取所有考试
    generateExam: async(courseId) => {
      return apiClient.get("/teacher/addPaper", {
        params:{
          courseId: courseId
        }
      })
    },

    /**
     * 智能生成考试试卷
     * @param {string} content - 考试内容要求
     * @param {string} courseId - 课程ID
     * @returns {Promise<object>} 考试创建结果
     */
    createIntelligentExam: async (content, courseId) => {
      return await apiClient.post('/teacher/ai/createExamByai', null, {
        params: { content, courseId }
      });
    },

    /**
     * 获取课程考试信息题目
     * @param {string} examId - 考试ID
     * @returns {Promise<object>} 考试题目信息
     */
    getCourseExamInfo: async (examId) => {
      return await apiClient.get('/course/getCourseExamInfo', {
        params: { examId }
      });
    },

    /**
     * 删除考试
     * @param {string} examId - 考试ID
     * @returns {Promise<object>} 删除结果
     */
    deleteExamById: async (examId) => {
      return await apiClient.delete(`/teacher/deleteExam/${examId}`);
    },

    /**
     * 发布试卷
     * @param {string} examId - 考试ID
     * @returns {Promise<object>} 发布结果
     */
    releasePaper: async (examId) => {
      return await apiClient.post('/teacher/releasePaper', null, {
        params: { examId }
      });
    },

    /**
     * AI阅卷
     * @param {string} studentId - 学生ID
     * @param {string} examId - 考试ID
     * @returns {Promise<object>} 阅卷结果
     */
    aiMarkingExam: async (studentId, examId) => {
      return await apiClient.post('/teacher/ai/aiMarkingExam', null, {
        params: { studentId, examId }
      });
    },

    // 获取考核提交列表
    getSubmissions: async (assessmentId) => {
      return await apiClient.get(`/teacher/assessment/${assessmentId}/submissions`);
    },

    // 获取单个提交详情
    getSubmissionDetail: async (assessmentId, submissionId) => {
      return await apiClient.get(`/teacher/assessment/${assessmentId}/submissions/${submissionId}`);
    },

    // 保存评分结果
    saveGrading: async (params) => {
      return await apiClient.post('/teacher/assessment/save-grading', params);
    },

    /**
     * 根据章节ID和课程ID查询所有材料信息
     * @param {string} chapterId - 章节ID
     * @param {string} courseId - 课程ID
     * @returns {Promise<object>} 材料信息
     */
    getAllMaterialsByChapterAndCourse: async (chapterId, courseId) => {
      return await apiClient.get('/chapter/selectAllMaterialBypointId', {
        params: { chapterId, courseId }
      });
    },
  },

  /**
   * 创建课程资源
   * @param {object} createMaterialDTO - 课程资源创建数据
   * @returns {Promise<object>} 创建结果
   */
  createMaterial: async (createMaterialDTO) => {
    return await apiClient.post('/material/create', createMaterialDTO);
  },

  /**
   * 根据课程ID获取课程资源
   * @param {string} courseId - 课程ID
   * @returns {Promise<object>} 课程资源列表
   */
  getMaterialByCourseId: async (courseId) => {
    return await apiClient.get('/material/get', {
      params: { courseId }
    });
  },

  /**
   * 删除课程资源
   * @param {string} materialId - 资源ID
   * @returns {Promise<object>} 删除结果
   */
  deleteMaterial: async (materialId) => {
    return await apiClient.delete('/material/delete', {
      params: { materialId }
    });
  },

  /**
   * 根据课程ID获取教案信息
   * @param {string} courseId - 课程ID
   * @returns {Promise<object>} 教案信息列表
   */
  getLessonPlanByCourseId: async (courseId) => {
    return await apiClient.get('/material/getLessonPlan', {
      params: { courseId }
    });
  },

  /**
   * 更新教案内容
   * @param {object} lessonPlan - 教案对象
   * @returns {Promise<object>} 更新结果
   */
  updateLessonPlan: async (lessonPlan) => {
    return await apiClient.put('/material/updateLessonPlan', lessonPlan);
  },

  /**
   * 更新教案状态
   * @param {string} lessonPlanId - 教案ID
   * @param {string} status - 状态值
   * @returns {Promise<object>} 更新结果
   */
  updateLessonPlanStatus: async (lessonPlanId, status) => {
    return await apiClient.get('/material/updateLessonPlanStatus', {
      params: { lessonPlanId, status }
    });
  },

  /**
   * 更新教案状态为待审核
   * @param {string} lessonPlanId - 教案ID
   * @returns {Promise<object>} 更新结果
   */
  updateLessonPlanToPending: async (lessonPlanId) => {
    return await apiClient.put('/material/updateLessonPlanToPending', null, {
      params: { lessonPlanId }
    });
  },

  /**
   * 删除教案
   * @param {string} lessonPlanId - 教案ID
   * @returns {Promise<object>} 删除结果
   */
  deleteLessonPlan: async (lessonPlanId) => {
    return await apiClient.delete('/material/deleteLessonPlan', {
      params: { lessonPlanId }
    });
  },

  /**
   * 获取课程考试情况
   * @param {string} courseId - 课程ID
   * @returns {Promise<object>} 课程考试情况数据
   */
  getStudentExam: async (courseId) => {
    return await apiClient.get('/teacher/getStudentExam', {
      params: { courseId }
    });
  },

  /**
   * 获取课程作业情况
   * @param {string} courseId - 课程ID
   * @returns {Promise<object>} 课程作业情况数据
   */
  getStudentHomework: async (courseId) => {
    return await apiClient.get('/teacher/getStudentHomework', {
      params: { courseId }
    });
  },

  /**
   * 教师创建练习题
   * @param {string} content - 练习内容要求
   * @param {string} courseId - 课程ID
   * @param {string} chapterId - 章节ID
   * @returns {Promise<object>} 创建结果
   */
  teacherCreateTest: async (content, courseId, chapterId) => {
    return await apiClient.post('/teacher/ai/teacher/createPractice', null, {
      params: { content, courseId, chapterId }
    });
  },

  /**
   * 查询指定班级课程对应的学生整体知识点掌握情况
   * @param {string} couresId - 课程ID
   * @returns {Promise<object>} 班级错误信息数据
   */
  getAllClassNotCorrectInfo: async (couresId) => {
    return await apiClient.get('/teacher/getAllClassNotCorrectInfo', {
      params: { couresId }
    });
  },

  /**
   * 根据知识点ID获取知识点名称
   * @param {string} pointId - 知识点ID
   * @returns {Promise<object>} 知识点名称
   */
  getKnowledgeNameById: async (PointId) => {
    return await apiClient.get('/knowledge/getKonwledgeNameById', {
      params: { PointId }
    });
  },

  /**
   * 获取考试学生信息
   * @param {string} examId - 考试ID
   * @returns {Promise<object>} 考试学生信息列表
   */
  getExamStudentInfo: async (examId) => {
    return await apiClient.get(`/teacher/getExamStudentInfo/${examId}`);
  },

  /**
   * 查询特定班级的知识点的高频错误知识点信息
   * @param {string} couresId - 课程ID
   * @returns {Promise<object>} 高频错误知识点列表
   */
  getTheMaxUncorrectPoint: async (couresId) => {
    return await apiClient.get('/teacher/getTheMaxUncorrectPoint', {
      params: { couresId }
    });
  },

  /**
   * 获取课程整体情况
   * @param {number} courseId - 课程ID
   * @param {number} examId - 考试ID
   * @returns {Promise<object>} 课程整体情况数据
   */
  getAllSituation: async (courseId, examId) => {
    return await apiClient.get(`/teacher/getAllSituation/${courseId}`, {
      params: { examId }
    });
  },
};

//管理员服务
export const adminService = {
  //获取全部用户信息
  getAllUsers: async () => {
    return await apiClient.get("/admin/getAllUserInfo");
  },

  editUser: async (userData) => {
    return await apiClient.put(`/admin/updateUserInfo`, {
      userId: userData.userId,
      realName: userData.realName,
      userType: userData.userType,
      email: userData.email,
    });
  },

  deleteUser: async (userId) => {
    return await apiClient.delete(`/admin/deleteInformation/${userId}`);
  },

  getResource: async () => {
    return await apiClient.post(`/admin/getAllResources`);
  },

  deleteResource: async (resourceId) => {
    return await apiClient.delete(`/admin/deleteResource/${resourceId}`);
  },

  getUseTimes: async () => {
    return await apiClient.post(`/admin/Information`);
  },

  getErrorKnow: async () => {
    return await apiClient.get(`/admin/getHighFrequencyErrorPoints`);
  },

  getAllPaper: async (courseId) => {
    return apiClient.get("/teacher/addPaper", {
      params: {
        courseId: courseId,
      },
    });
  },

  getQuestion: async (examId) => {
    return apiClient.get("/course/getCourseExamInfo", {
      params: {
        examId: examId,
      },
    });
  },

  createCourse: async (courseData) => {
    return apiClient.post("/course/createCourse", null, {
      params: {
        courseName: courseData.title,
        semester: courseData.time
      }
    });
  },

  autoAssignTeachers: async (courseId) => {
    return apiClient.post(`/course/autoAssignTeacher?courseId=${courseId}`);
  },

  /**
   * 更新考试状态
   * @param {string} examId - 考试ID
   * @returns {Promise<object>} 更新结果
   */
  updateExamStatusById: async (examId) => {
    return await apiClient.post('/teacher/updateExamStatus', null, {
      params: { examId }
    });
  },

  /**
   * 更换课程教师
   * @param {number} courseId - 课程ID
   * @param {string} teacherId - 教师ID
   * @returns {Promise<object>} 更换结果
   */
  changeTeacher: async (courseId, teacherId) => {
    return await apiClient.put('/course/changeTeacher', null, {
      params: { courseId, teacherId }
    });
  },

  /**
   * 获取所有课程(包含教师信息)
   * @returns {Promise<object>} 所有课程数据列表
   */
  getAllCourses: async () => {
    return await apiClient.get('/course/all');
  },

  /**
   * 删除课程
   * @param {number} courseId - 课程ID
   * @returns {Promise<object>} 删除结果
   */
  deleteCourse: async (courseId) => {
    return await apiClient.delete(`/course/delete/${courseId}`);
  },

  /**
   * 修改章节名称
   * @param {object} chapter - 章节信息，包含章节ID和新名称
   * @returns {Promise<object>} 修改结果
   */
  updateChapterName: async (chapter) => {
    return await apiClient.post('/chapter/updateChapterName', chapter);
  },

  /**
   * 删除章节
   * @param {string} chapterId - 章节ID
   * @returns {Promise<object>} 删除结果
   */
  deleteChapter: async (chapterId) => {
    return await apiClient.delete('/chapter/deleteChapter', {
      params: { chapterId }
    });
  },

  /**
   * 添加章节
   * @param {object} chapter - 章节信息，包含课程ID和章节名称
   * @returns {Promise<object>} 添加结果
   */
  addChapter: async (chapter) => {
    return await apiClient.post('/chapter/addChapter', chapter);
  },

  /**
   * 修改知识点名称
   * @param {object} knowledgePoint - 知识点信息，包含知识点ID和新名称
   * @returns {Promise<object>} 修改结果
   */
  updateKnowledgeName: async (knowledgePoint) => {
    return await apiClient.post('/knowledge/updateKnowledgeName', knowledgePoint);
  },

  /**
   * 删除知识点
   * @param {string} pointId - 知识点ID
   * @returns {Promise<object>} 删除结果
   */
  deleteKnowledgePoint: async (pointId) => {
    return await apiClient.delete('/knowledge/deleteKnowledgePoint', {
      params: { pointId }
    });
  },

  /**
   * 添加知识点
   * @param {object} knowledgePoint - 知识点信息
   * @param {string} chapterId - 章节ID
   * @returns {Promise<object>} 添加结果
   */
  addKnowledgePoint: async (knowledgePoint, chapterId) => {
    return await apiClient.post('/knowledge/addKnowledgePoint', knowledgePoint, {
      params: { chapterId }
    });
  },
};

// 学生服务
export const studentService = {
  /**
   * 根据userId获取student信息
   * @param {string} userId - 用户ID
   * @returns {Promise<object>} 学生信息
   */
  getStudentIdByUserId: async (userId) => {
    return await apiClient.get('/stu/get/studentIdByUserId', {
      params: { userId }
    });
  },

  /**
   * 根据agent获取对应的学习资源
   * @param {string} studentId - 学生ID
   * @param {string} pointId - 知识点ID
   * @returns {Promise<object>} 学习资源链接
   */
  getStudyByAgent: async (studentId, pointId) => {
    return await apiClient.get('/stu/get/studyByAgent', {
      params: { studentId, pointId }
    });
  },

  /**
   * 生成练习题目
   * @param {object} params 包含学生历史练习情况和练习要求的参数
   * @returns {Promise<object>} 生成的练习题目和纠错信息
   */
  generatePracticeQuestions: (params) => apiClient.post('/student/practice-evaluation', params),

  getStudentAnalysis: async () => {
    return await apiClient.get('/teacher/student-analysis');
  },

  /**
   * 获取学生错题分析数据
   * @returns {Promise<object>} 错题分析数据
   */
  getErrorAnalysis: async () => {
    return await apiClient.get('/knowledge/checkAllNOtCruuent'); // 假设后端接口为 /student/error-analysis
  },

  /**
   * 根据学号查询学生信息
   * @param {string} studentNumber - 学号
   * @returns {Promise<object>} 学生信息
   */
  findByStudentNumber: async (studentNumber) => {
    return await apiClient.get(`/stu/student/${studentNumber}`);
  },

  /**
   * 获取平均错误率
   * @returns {Promise<object>} 平均错误率数据
   */
  getAverageErrorRate: async () => {
    return await apiClient.get('/knowledge/getAver');
  },

  /**
   * 一键生成试题
   * @param {string} knowledgeId 知识点ID
   * @param {object} config 试题配置
   * @returns {Promise<object>} 生成的试题
   * //带权重
   */
  generateQuestions: async (knowledgeId, config) => {
    return await apiClient.post('/knowledge/generate-questions', {
      knowledgeId,
      config
    });
  },


  //获取错误知识点的细节
  getKnowlegeById: async (pointId) => {
    return await apiClient.get('/knowledge/getKnowlegeBypointId', {
      params: { pointId: pointId }
    });
  },

  //根据错误知识点生成题目不选择权重
  generateExamByKnowledgePoints: async (payload, params) => {
    // 注意：这里把pointIds从路径参数移动到请求体
    return await apiClient.post('/knowledge/ai/createTest', payload, {
      params: params
    });
  },


  //根据错误知识点生成带权重
  createTest: async (payload, params) => {
    return await apiClient.post('/knowledge/ai/t/createTest', payload, {
      params: params
    });
  },

  /**
   * 获取课程列表
   * @returns {Promise<Array>} 课程数据数组
   */
  getCourses: async () => {
    return await apiClient.get('/course/all');
  },

  //获取学生现有课程
  getOwnCourses: async() => {
    return await apiClient.get('/course/getAllStudentHaveCourse')
  },

  //选课
  selectCourse: async (courseId) => {
    return await apiClient.post(`/course/add/${courseId}`);
  },

  //退选课程
  unselectCourse: async (courseId) => {
    return await apiClient.post(`/course/exit/${courseId}`);
  },

  //获取学期
  getSemester: async() => {
    return await apiClient.get('/course/getAllLearnDate');
  },

  /**
   * 根据年份查询课程信息
   * @param {string} date - 年份/学期
   * @returns {Promise<object>} 课程信息列表
   */
  getCoursesByDate: async (date) => {
    return await apiClient.get('/course/getAllCourse', {
      params: { date }
    });
  },

  //获取学情分析
  getCourseAnalysis: async(courseId) => {
    return await apiClient.get('/stu/academicanalysis',{
      params: { courseId: courseId }
    })
  },

  //根据课程获取其下的章节信息与完成度
  getChapterInfo: async(courseId) => {
    return await apiClient.get("/chapter/selectchapter",{
      params: {
        courseId: courseId
      }
    });
  },

  //根据章节获取旗下知识点
  getChapterKnow: async(chaptId) =>{
    return await apiClient.get("/chapter/selectAllKnowledgeBychaptId",{
      params:{
        chaptId: chaptId
      }
    })
  },

  //根据课程获取所有错题
  getAllErrors: async(courseId) =>{
    return await apiClient.get("/chapter/getAllNotcoreectTest", {
      params:{
        courseId: courseId
      }
    })
  },

  //获取所有课件等资源
  getAllResources: async(chapterId, courseId) =>{
    return await apiClient.get("/chapter/selectAllMaterialBypointId", {
      params:{
        chapterId: chapterId,
        courseId: courseId
      }
    })
  },

  // 获取社区帖子
  getAllPosts: async() => {
    return await apiClient.get("/community/all")
  },

  // 获取社区帖子下的评论
  getAllComments: async(postId) => {
    return await apiClient.get(`/community/comments/${postId}`)
  },

  // 根据用户获取社区帖子
  getOwnPosts: async(userId) => {
    return await apiClient.get(`/community/all/${userId}`)
  },

  // 新建社区帖子
  createPost: async (postDTO) => {
    return await apiClient.post('/community/post', postDTO);
  },

  // 评论社区
  createComment: async (commentDTO) => {
    return await apiClient.post('/community/add/comments', commentDTO);
  },

  // 点赞帖子
  likePost: async (postId) => {
    return await apiClient.post(`/community/${postId}/like`);
  },

  // 取消点赞帖子
  unlikePost: async (postId) => {
    return await apiClient.post(`/community/${postId}/unlike`);
  },

  //获取作业信息
  getAllWork: async( chapterId, courseId) => {
    return await apiClient.get("/chapter/selectAllTextByChapterId", {
      params:{
        chapterId: chapterId,
        courseId: courseId
      }
    })
  },

  // 在 studentService 中添加以下方法

  /**
   * 获取学生考试信息
   * @param {string} examId - 考试ID
   * @param {string} studentId - 学生ID
   * @returns {Promise<object>} 考试题目详情
   */
  getCourseExamInfo: async (examId, studentId) => {
    return await apiClient.get('/course/getCourseExamStudent', {
      params: {
        examId,
        studentId
      }
    });
  },

  //提交学生答案
  submitStudentAnswer: async(data) =>{
    return apiClient.post('/chapter/setStudentAwser', data)
  },

  //获取所有考试
  getAllExams: async(courseId) => {
    return apiClient.get("/teacher/addPaper", {
      params:{
        courseId: courseId
      }
    })
  },

  //检测学生答案
  judgeStudentAnswer: async(studentTextAnswerDTO) => {
    return apiClient.get("/chapter/ju/test", {
      params: {
        studentTextAnswerDTO: studentTextAnswerDTO
      }
    })
  },

  getTeachers: async () => {
    return await apiClient.get('/studentteacher/chat/getAllTeacherInfo');
  },

  //获取用户信息
  /**
   * 师生对话相关接口
   */
  teacherChat: {
    /**
     * 建立师生连接
     * @param {String} studentId - 学生ID
     * @param {String} teacherId - 教师ID
     * @returns {Promise<Long>} 接口返回的结果ID
     */
    setConnection: async (studentId, teacherId) => {
      return await apiClient.post('/studentteacher/chat/setConnection', null, {
        params: {
          studentId: studentId,
          teacherId: teacherId
        }
      });
    },

    /**
     * 获取老师列表
     * @returns {Promise<Array>} 老师列表
     */
    getTeachers: async (courseIds) => {
      return await apiClient.post('/studentteacher/chat/getAllTeacherInfo', courseIds);
    },

    /**
     * 获取在线老师列表
     * @returns {Promise<Array>} 在线老师列表
     */
    getOnlineTeachers: async () => {
      return await apiClient.get('/api/teacher-chat/online-teachers');
    },

    /**
     * 发送消息
     * @param {Object} messageData - 消息数据
     * @returns {Promise<Object>} 发送结果
     */
    sendMessage: async (messageData) => {
      return await apiClient.post('/api/teacher-chat/send-message', messageData);
    },

    /**
     * 获取聊天历史
     * @param {Number} conversationId - 会话ID
     * @returns {Promise<Object>} 聊天历史
     */
    getChatHistory: async (teacherId) => {
      return await apiClient.get('/studentteacher/chat/getChatByteacherId', {
        params: { teacherId }
      });
    },

    /**
     * 标记消息为已读
     * @returns {Promise<Object>} 标记结果
     */
    markMessagesAsRead1: async () => {
      return await apiClient.get('/studentteacher/chat/getAllChatNotReadInfoBytudent');
    },

    /**
     * 标记消息为已读
     * @returns {Promise<Object>} 标记结果
     */
    markMessagesAsRead2: async () => {
      return await apiClient.get('/studentteacher/chat/getAllChatNotReadInfoByteacher');
    },

    /**
     * 根据教师ID获取用户ID
     * @param {number} teacherId - 教师ID
     * @returns {Promise<Object>} 用户ID信息
     */
    getUserIdByTeacher: async (teacherId) => {
      return await apiClient.get('teacher/getUserIdByteacher', {
        params: { teacherId }
      });
    }
  }
};

// 认证服务
export const authService = {
  login: async (credentials) => {
    return await apiClient.post('/common/login', credentials);
  },
  register: async (userData) => {
    return await apiClient.post('/common/register', userData);
  },
  sendVerificationCode: async (type, value) => {
    return await apiClient.post('/auth/send-code', { type, value });
  },
  resetPassword: async (resetData) => {
    return await apiClient.post('/common/password/reset', resetData);
  },
  logout: async (userId) => {
    return await apiClient.post('/common/logout', null, {
      params: { userId }
    });
  }
};

export const chatService = {
  sendMessage: async (payload) => {
    return await apiClient.post('/chat/userchat', payload);
  }
};

export const isOnline = async (userId) => {
  return await apiClient.get(`/api/chat/is-online/${userId}`);
};



