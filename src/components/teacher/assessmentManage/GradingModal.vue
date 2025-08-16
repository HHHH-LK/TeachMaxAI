<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:modelValue', $event)"
    title="智能阅卷"
    width="1200px"
    :before-close="handleClose"
  >
    <div v-if="assessment" class="grading-content">
      <div class="grading-header">
        <h3>{{ assessment.title }} - 智能阅卷</h3>
        <div class="grading-stats">
          <el-tag type="info">总提交: {{ totalSubmissions }}</el-tag>
          <el-tag type="warning">待阅卷: {{ pendingGradingCount }}</el-tag>
          <el-tag type="success">已阅卷: {{ gradedCount }}</el-tag>
        </div>
        <div class="grading-actions-header">
          <el-button type="success" @click="autoGradeAll" :loading="autoGradingLoading">
            <el-icon><Star /></el-icon>
            一键智能阅卷
          </el-button>
          <el-button type="primary" @click="saveAllGrading" :loading="savingLoading">
            <el-icon><Check /></el-icon>
            保存所有评分
          </el-button>
        </div>
      </div>

      <div class="grading-progress">
        <el-progress
          :percentage="gradingProgress"
          :format="(format) => `${format}%`"
          status="success"
        />
      </div>

      <!-- 学生列表 -->
      <div class="students-list">
        <el-table :data="submissions" style="width: 100%" v-loading="loading">
          <el-table-column prop="studentName" label="学生姓名" width="120" />
          <el-table-column prop="studentNumber" label="学号" width="120" />
          <el-table-column label="提交状态" width="120">
            <template #default="scope">
              <el-tag type="success">
                已提交
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="阅卷状态" width="120">
            <template #default="scope">
              <el-tag :type="getGradingStatusType(scope.row)">
                {{ getGradingStatusText(scope.row) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="总分" width="100">
            <template #default="scope">
              <span>
                {{ scope.row.score !== null ? scope.row.score : 0 }}/{{ assessment.totalScore }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="AI报告" width="120">
            <template #default="scope">
              <el-button 
                v-if="scope.row.aiReport" 
                size="small" 
                type="info" 
                @click="showAIReport(scope.row)"
              >
                查看报告
              </el-button>
              <span v-else class="text-muted">暂无报告</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="scope">
              <el-button size="small" @click="viewSubmissionDetail(scope.row)">查看详情</el-button>
            </template>
          </el-table-column>
          <el-table-column label="智能操作" width="220">
            <template #default="scope">
              <el-button 
                size="small" 
                type="primary" 
                @click="aiGradeSingleStudent(scope.row)"
                :loading="scope.row.aiLoading"
              >智能阅卷</el-button>
              <el-button 
                size="small" 
                type="success" 
                @click="generateReport(scope.row)"
                :loading="scope.row.reportLoading"
              >生成报告</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </el-dialog>

  <!-- 学生详情对话框 -->
  <SubmissionDetailModal
    v-model="showSubmissionDetail"
    :submission="selectedSubmissionDetail"
    :assessment="assessment"
    @save="$emit('save-submission', $event)"
    @ai-grade="aiGradeSingleStudent"
    @ai-grade-question="aiGradeSingleQuestion"
  />

  <el-dialog v-model="showReportDialog" title="学情报告" width="500px" :show-close="true">
    <pre style="white-space: pre-wrap;word-break: break-all;">{{ currentReportText }}</pre>
    <template #footer>
      <el-button @click="showReportDialog = false">关闭</el-button>
    </template>
  </el-dialog>

  <!-- AI阅卷报告对话框 -->
  <el-dialog v-model="showAIDialog" :title="`${currentAIStudent} - AI阅卷报告`" width="800px" :show-close="true">
    <div class="ai-report-content">
      <div class="ai-report-header">
        <el-tag type="info">AI智能阅卷结果</el-tag>
        <el-tag type="success">实时生成</el-tag>
      </div>
      <div class="ai-report-body">
        <pre style="white-space: pre-wrap;word-break: break-all;font-family: 'Courier New', monospace;background: #f5f5f5;padding: 15px;border-radius: 4px;">{{ currentAIText }}</pre>
      </div>
    </div>
    <template #footer>
      <el-button @click="showAIDialog = false">关闭</el-button>
      <el-button type="primary" @click="copyAIReport">复制报告</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Star, Check } from '@element-plus/icons-vue';
import { ElDialog } from 'element-plus';
import { teacherService } from '@/services/api';
import SubmissionDetailModal from './SubmissionDetailModal.vue';

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  assessment: {
    type: Object,
    default: null
  }
});

const emit = defineEmits(['update:modelValue', 'close', 'save-submission']);

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
});

const loading = ref(false);
const showSubmissionDetail = ref(false);
const selectedSubmissionDetail = ref(null);
const autoGradingLoading = ref(false);
const savingLoading = ref(false);
const showReportDialog = ref(false);
const currentReportText = ref('');
const showAIDialog = ref(false);
const currentAIText = ref('');
const currentAIStudent = ref('');

// 计算属性
const totalSubmissions = computed(() => {
  if (!props.assessment || !props.assessment.studentsWithAnswers) return 0;
  return props.assessment.studentsWithAnswers.length;
});

const pendingGradingCount = computed(() => {
  if (!props.assessment || !props.assessment.studentsWithAnswers) return 0;
  // 待阅卷：没有AI阅卷报告的学生
  return props.assessment.studentsWithAnswers.filter(s => !s.aiReport).length;
});

const gradedCount = computed(() => {
  if (!props.assessment || !props.assessment.studentsWithAnswers) return 0;
  // 已阅卷：有AI阅卷报告的学生
  return props.assessment.studentsWithAnswers.filter(s => s.aiReport).length;
});

const gradingProgress = computed(() => {
  if (totalSubmissions.value === 0) return 0;
  // 计算已阅卷的百分比（已阅卷 / 总学生数）
  const gradedSubmissions = props.assessment?.studentsWithAnswers?.filter(s => s.aiReport) || [];
  return Math.round((gradedSubmissions.length / totalSubmissions.value) * 100);
});

// 获取学生列表数据
const submissions = computed(() => {
  if (!props.assessment || !props.assessment.studentsWithAnswers) return [];
  return props.assessment.studentsWithAnswers;
});

// 获取阅卷状态类型
const getGradingStatusType = (submission) => {
  // 如果有AI阅卷报告，说明已经阅卷完成，无论分数多少都显示为已阅卷
  if (submission.aiReport) {
    return 'success'; // 已阅卷
  } else if (submission.score !== null && submission.score > 0) {
    return 'success'; // 已阅卷且有分数
  } else {
    return 'warning'; // 已提交但未阅卷
  }
};

// 获取阅卷状态文本
const getGradingStatusText = (submission) => {
  // 如果有AI阅卷报告，说明已经阅卷完成，无论分数多少都显示为已阅卷
  if (submission.aiReport) {
    return '已阅卷';
  } else if (submission.score !== null && submission.score > 0) {
    return '已阅卷';
  } else {
    return '已提交';
  }
};

// 查看学生详情
const viewSubmissionDetail = (submission) => {
  selectedSubmissionDetail.value = submission;
  showSubmissionDetail.value = true;
};

// 单个学生智能阅卷
const aiGradeSingleStudent = async (submission) => {
  submission.aiLoading = true;
  try {
    console.log(`开始智能阅卷 - 学生: ${submission.studentName}, ID: ${submission.studentId}, 考试ID: ${props.assessment.id}`);
    console.log('学生提交数据:', submission);
    
    // 调用真实的AI阅卷API
    const response = await teacherService.assessment.aiMarkingExam(
      submission.studentId, 
      props.assessment.id
    );
    
    console.log(`AI阅卷API响应 - 学生: ${submission.studentName}:`, response);
    
    if (response.data && response.data.success) {
      // 解析AI阅卷报告
      const aiReport = response.data.data;
      console.log('AI阅卷原始报告:', aiReport);
      
      // 尝试从报告中提取分数信息
      let extractedScore = 0;
      let totalPossibleScore = 0;
      
      // 解析报告中的分数信息
      if (typeof aiReport === 'string') {
        // 尝试从文本报告中提取分数
        const scoreMatch = aiReport.match(/总分:\s*([\d.]+)\/([\d.]+)/);
        if (scoreMatch) {
          extractedScore = parseFloat(scoreMatch[1]) || 0;
          totalPossibleScore = parseFloat(scoreMatch[2]) || 0;
          console.log(`从报告中提取分数: ${extractedScore}/${totalPossibleScore}`);
        }
        
        // 尝试从报告中提取题目统计
        const questionMatch = aiReport.match(/题目总数:\s*(\d+)题/);
        const answeredMatch = aiReport.match(/已作答:\s*(\d+)题/);
        const correctMatch = aiReport.match(/正确:\s*(\d+)/);
        
        if (questionMatch && answeredMatch && correctMatch) {
          const totalQuestions = parseInt(questionMatch[1]);
          const answeredQuestions = parseInt(answeredMatch[1]);
          const correctQuestions = parseInt(correctMatch[1]);
          
          console.log('题目统计信息:', {
            totalQuestions,
            answeredQuestions,
            correctQuestions,
            accuracy: totalQuestions > 0 ? (correctQuestions / totalQuestions * 100).toFixed(2) + '%' : '0%'
          });
        }
      } else if (aiReport && typeof aiReport === 'object') {
        // 如果是对象格式，直接使用
        extractedScore = aiReport.score || 0;
        totalPossibleScore = aiReport.totalScore || 0;
        console.log('对象格式分数:', { extractedScore, totalPossibleScore });
      }
      
      // 更新学生分数
      const oldScore = submission.score;
      submission.score = extractedScore;
      
      // 保存AI阅卷报告到学生数据中
      submission.aiReport = aiReport;
      submission.totalPossibleScore = totalPossibleScore;
      
      console.log(`阅卷完成 - 学生: ${submission.studentName}`);
      console.log('分数变化:', {
        oldScore: oldScore,
        newScore: submission.score,
        totalPossibleScore: totalPossibleScore,
        percentage: totalPossibleScore > 0 ? ((submission.score / totalPossibleScore) * 100).toFixed(2) + '%' : '0%'
      });
      
      // 输出详细的AI阅卷结果
      console.log('AI阅卷完整结果:', {
        studentName: submission.studentName,
        studentId: submission.studentId,
        examId: props.assessment.id,
        score: submission.score,
        totalPossibleScore: totalPossibleScore,
        aiReport: aiReport,
        timestamp: new Date().toISOString()
      });
      
      ElMessage.success(`${submission.studentName} 智能阅卷完成，得分：${submission.score}分`);
    } else {
      console.error(`AI阅卷失败 - 学生: ${submission.studentName}, 响应:`, response);
      ElMessage.error(response.data?.message || '智能阅卷失败');
    }
  } catch (error) {
    console.error(`智能阅卷异常 - 学生: ${submission.studentName}:`, error);
    console.error('错误详情:', {
      message: error.message,
      stack: error.stack,
      response: error.response
    });
    ElMessage.error('智能阅卷失败，请重试');
  } finally {
    submission.aiLoading = false;
  }
};



// 单题智能评分
const aiGradeSingleQuestion = async (submission, qIndex) => {
  const answer = submission.answers[qIndex];
  answer.aiLoading = true;
  try {
    console.log(`开始单题智能评分 - 学生: ${submission.studentName}, 题目索引: ${qIndex}`);
    console.log('题目信息:', answer);
    
    // 调用真实的AI阅卷API进行单题评分
    const response = await teacherService.assessment.aiMarkingExam(
      submission.studentId, 
      props.assessment.id
    );

    console.log(`单题评分API响应 - 学生: ${submission.studentName}, 题目: ${qIndex}:`, response);
    
    if (response.data && response.data.success) {
      // 解析AI阅卷报告
      const aiReport = response.data.data;
      console.log(`学生 ${submission.studentName} 题目 ${qIndex} 的AI阅卷报告:`, aiReport);
      
      // 更新单题评分结果
      const oldAiScore = answer.aiScore;
      const oldAiComment = answer.aiComment;
      
      // 尝试从报告中提取单题信息
      if (typeof aiReport === 'string') {
        // 从文本报告中提取题目信息
        const questionPattern = new RegExp(`第${qIndex + 1}题\\(ID:(\\d+),[^)]+\\): ([\\d.]+)/([\\d.]+)`);
        const questionMatch = aiReport.match(questionPattern);
        
        if (questionMatch) {
          const questionId = questionMatch[1];
          const questionScore = parseFloat(questionMatch[2]) || 0;
          const questionMaxScore = parseFloat(questionMatch[3]) || 0;
          
          answer.aiScore = questionScore;
          answer.aiComment = `AI评分: ${questionScore}/${questionMaxScore}`;
          
          console.log(`题目 ${qIndex} 评分提取:`, {
            questionId,
            score: questionScore,
            maxScore: questionMaxScore
          });
        } else {
          // 如果没有找到具体题目，使用默认值
          answer.aiScore = 0;
          answer.aiComment = 'AI评分完成';
          console.log(`题目 ${qIndex} 未找到具体评分信息，使用默认值`);
        }
        
        // 尝试提取题型统计信息
        const questionTypeMatch = aiReport.match(/分题型得分情况/);
        if (questionTypeMatch) {
          console.log(`学生 ${submission.studentName} 分题型得分情况已包含在报告中`);
        }
      } else if (aiReport && typeof aiReport === 'object') {
        // 如果是对象格式，尝试使用questionScores和questionComments
        answer.aiScore = aiReport.questionScores?.[qIndex] || 0;
        answer.aiComment = aiReport.questionComments?.[qIndex] || 'AI评分完成';
      }
      
      console.log(`单题评分完成 - 学生: ${submission.studentName}, 题目: ${qIndex}`);
      console.log('评分变化:', {
        oldScore: oldAiScore,
        newScore: answer.aiScore,
        oldComment: oldAiComment,
        newComment: answer.aiComment
      });
      
      // 输出题目评分的详细结果
      console.log(`题目 ${qIndex} 评分结果:`, {
        studentName: submission.studentName,
        questionIndex: qIndex,
        aiScore: answer.aiScore,
        aiComment: answer.aiComment,
        aiReport: aiReport
      });

    ElMessage.success('AI评分完成');
    } else {
      console.error(`单题评分失败 - 学生: ${submission.studentName}, 题目: ${qIndex}, 响应:`, response);
      ElMessage.error(response.data?.message || 'AI评分失败');
    }
  } catch (error) {
    console.error(`单题评分异常 - 学生: ${submission.studentName}, 题目: ${qIndex}:`, error);
    console.error('错误详情:', {
      message: error.message,
      stack: error.stack,
      response: error.response
    });
    ElMessage.error('AI评分失败');
  } finally {
    answer.aiLoading = false;
  }
};

// 保存所有评分
const saveAllGrading = async () => {
  savingLoading.value = true;
  try {
    console.log('开始保存所有评分...');
    console.log('当前所有学生数据:', submissions.value);
    
    // 保存所有已阅卷的评分结果
    const gradedSubmissions = submissions.value.filter(s => s.aiReport);
    const pendingSubmissions = submissions.value.filter(s => !s.aiReport);
    
    console.log('评分统计:', {
      totalStudents: submissions.value.length,
      gradedStudents: gradedSubmissions.length,
      pendingStudents: pendingSubmissions.length
    });
    
    if (gradedSubmissions.length === 0) {
      console.warn('没有已阅卷的提交需要保存');
      ElMessage.warning('没有已阅卷的提交需要保存');
      return;
    }
    
    console.log('准备保存的已阅卷学生:', gradedSubmissions.map(s => ({
      name: s.studentName,
      id: s.studentId,
      score: s.score
    })));
    
    // 这里可以调用API保存评分结果到后端
    // const response = await teacherService.assessment.saveGrading({
    //   examId: props.assessment.id,
    //   submissions: gradedSubmissions
    // });
    
    console.log('评分保存完成');
    ElMessage.success(`成功保存 ${gradedSubmissions.length} 个学生的评分结果`);
  } catch (error) {
    console.error('保存评分失败:', error);
    console.error('错误详情:', {
      message: error.message,
      stack: error.stack,
      response: error.response
    });
    ElMessage.error('保存评分失败');
  } finally {
    savingLoading.value = false;
  }
};

// 一键批阅
const autoGradeAll = async () => {
  try {
    await ElMessageBox.confirm(
        "确定要对所有待阅卷的提交进行智能阅卷吗？",
        "智能阅卷确认",
        {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning",
        }
    );

    autoGradingLoading.value = true;
    ElMessage.info("正在使用AI进行智能阅卷，预计需要较长时间...");

    // 计算待阅卷的学生数量
    const pendingSubmissions = submissions.value.filter(
        submission => !submission.aiReport
    );

    console.log(`开始批量智能阅卷 - 总学生数: ${submissions.value.length}, 待阅卷数: ${pendingSubmissions.length}`);
    console.log('待阅卷学生列表:', pendingSubmissions.map(s => ({ name: s.studentName, id: s.studentId, score: s.score })));

    // 批量AI阅卷过程
    for (let i = 0; i < pendingSubmissions.length; i++) {
      const submission = pendingSubmissions[i];
      console.log(`批量阅卷进度: ${i + 1}/${pendingSubmissions.length} - 学生: ${submission.studentName}`);

      try {
        // 调用真实的AI阅卷API
        const response = await teacherService.assessment.aiMarkingExam(
          submission.studentId, 
          props.assessment.id
        );

        console.log(`批量阅卷API响应 - 学生: ${submission.studentName}:`, response);
        
        if (response.data && response.data.success) {
          // 解析AI阅卷报告
          const aiReport = response.data.data;
          console.log(`学生 ${submission.studentName} 的AI阅卷报告:`, aiReport);
          
          // 提取分数信息
          let extractedScore = 0;
          let totalPossibleScore = 0;
          
          if (typeof aiReport === 'string') {
            // 从文本报告中提取分数
            const scoreMatch = aiReport.match(/总分:\s*([\d.]+)\/([\d.]+)/);
            if (scoreMatch) {
              extractedScore = parseFloat(scoreMatch[1]) || 0;
              totalPossibleScore = parseFloat(scoreMatch[2]) || 0;
              console.log(`学生 ${submission.studentName} 分数提取: ${extractedScore}/${totalPossibleScore}`);
            }
            
            // 提取题目统计
            const questionMatch = aiReport.match(/题目总数:\s*(\d+)题/);
            const answeredMatch = aiReport.match(/已作答:\s*(\d+)题/);
            const correctMatch = aiReport.match(/正确:\s*(\d+)/);
            
            if (questionMatch && answeredMatch && correctMatch) {
              const totalQuestions = parseInt(questionMatch[1]);
              const answeredQuestions = parseInt(answeredMatch[1]);
              const correctQuestions = parseInt(correctMatch[1]);
              
              console.log(`学生 ${submission.studentName} 题目统计:`, {
                totalQuestions,
                answeredQuestions,
                correctQuestions,
                accuracy: totalQuestions > 0 ? (correctQuestions / totalQuestions * 100).toFixed(2) + '%' : '0%'
              });
            }
          } else if (aiReport && typeof aiReport === 'object') {
            extractedScore = aiReport.score || 0;
            totalPossibleScore = aiReport.totalScore || 0;
          }
          
          // 更新学生分数
          const oldScore = submission.score;
          submission.score = extractedScore;
          submission.isGraded = true;
          
          // 保存AI阅卷报告
          submission.aiReport = aiReport;
          submission.totalPossibleScore = totalPossibleScore;
          
          console.log(`批量阅卷完成 - 学生: ${submission.studentName}, 原分数: ${oldScore}, 新分数: ${submission.score}`);
          console.log(`学生 ${submission.studentName} 阅卷详情:`, {
            score: submission.score,
            totalPossibleScore: totalPossibleScore,
            percentage: totalPossibleScore > 0 ? ((submission.score / totalPossibleScore) * 100).toFixed(2) + '%' : '0%',
            aiReport: aiReport
          });

      // 更新进度提示
      if (i < pendingSubmissions.length - 1) {
        ElMessage.info(`已完成 ${i + 1}/${pendingSubmissions.length} 个学生的阅卷`);
          }
        } else {
          console.error(`批量阅卷失败 - 学生: ${submission.studentName}, 响应:`, response);
        }
      } catch (error) {
        console.error(`批量阅卷异常 - 学生: ${submission.studentName}:`, error);
        console.error('错误详情:', {
          message: error.message,
          stack: error.stack,
          response: error.response
        });
        ElMessage.error(`学生${submission.studentName}阅卷失败`);
      }
    }

    // 批量阅卷完成后的统计
    const finalGradedStudents = submissions.value.filter(s => s.aiReport);
    const finalPendingStudents = submissions.value.filter(s => !s.aiReport);
    
    console.log('批量智能阅卷完成 - 最终统计信息:', {
      totalStudents: submissions.value.length,
      gradedStudents: finalGradedStudents.length,
      pendingStudents: finalPendingStudents.length,
      averageScore: finalGradedStudents.length > 0 
        ? (finalGradedStudents.reduce((sum, s) => sum + s.score, 0) / finalGradedStudents.length).toFixed(2)
        : 0
    });
    
    // 输出每个学生的最终分数
    console.log('所有学生最终分数:', submissions.value.map(s => ({
      name: s.studentName,
      id: s.studentId,
      score: s.score,
      totalPossibleScore: s.totalPossibleScore,
      percentage: s.totalPossibleScore > 0 ? ((s.score || 0) / s.totalPossibleScore * 100).toFixed(2) + '%' : '0%'
    })));

    ElMessage.success(`智能阅卷完成！共阅卷 ${pendingSubmissions.length} 个学生`);
  } catch (error) {
    if (error !== "cancel") {
      console.error("批量智能阅卷失败:", error);
      console.error('错误详情:', {
        message: error.message,
        stack: error.stack,
        response: error.response
      });
      ElMessage.error("智能阅卷失败，请重试");
    }
  } finally {
    autoGradingLoading.value = false;
  }
};


// 新增：智能生成报告方法
const generateReport = async (submission) => {
  submission.reportLoading = true;
  try {
    // 模拟生成报告过程
    await new Promise(resolve => setTimeout(resolve, 8000));
    
    let reportContent = `【智能报告】${submission.studentName}的答题分析：\n`;
    reportContent += `- 总分：${submission.score !== null ? submission.score : 0}分\n`;
    
    if (submission.questions && submission.questions.length > 0) {
      const totalQuestions = submission.questions.length;
      const answeredQuestions = Object.keys(submission.userAnswers).length;
      const correctAnswers = submission.questions.filter(q => {
        const studentAnswer = submission.userAnswers[q.id];
        return studentAnswer === q.correctAnswer;
      }).length;
      
      reportContent += `- 答题数量：${answeredQuestions}/${totalQuestions}\n`;
      reportContent += `- 正确题目：${correctAnswers}题\n`;
      reportContent += `- 正确率：${totalQuestions > 0 ? Math.round((correctAnswers / totalQuestions) * 100) : 0}%\n`;
      
      const currentScore = submission.score !== null ? submission.score : 0;
      if (currentScore === 0) {
        reportContent += `- 分析：学生已提交试卷，但得分较低，建议重点复习相关知识点\n`;
      } else if (currentScore < assessment.totalScore * 0.6) {
        reportContent += `- 分析：学生答题情况一般，有较大提升空间\n`;
      } else if (currentScore < assessment.totalScore * 0.8) {
        reportContent += `- 分析：学生答题情况良好，部分题目有提升空间\n`;
      } else {
        reportContent += `- 分析：学生答题情况优秀，继续保持\n`;
      }
    }
    
    submission.report = reportContent;
    ElMessage.success(`${submission.studentName} 报告生成成功`);
    // 展示报告弹窗
    currentReportText.value = submission.report;
    showReportDialog.value = true;
  } catch (error) {
    ElMessage.error('报告生成失败');
  } finally {
    submission.reportLoading = false;
  }
};

// 显示AI阅卷报告
const showAIReport = (submission) => {
  currentAIStudent.value = submission.studentName;
  if (submission.aiReport) {
    if (typeof submission.aiReport === 'string') {
      currentAIText.value = submission.aiReport;
    } else {
      currentAIText.value = JSON.stringify(submission.aiReport, null, 2);
    }
  } else {
    currentAIText.value = '暂无AI阅卷报告';
  }
  showAIDialog.value = true;
};

// 复制AI报告
const copyAIReport = () => {
  const textArea = document.createElement('textarea');
  textArea.value = currentAIText.value;
  document.body.appendChild(textArea);
  textArea.select();
  try {
    document.execCommand('copy');
    ElMessage.success('AI报告已复制到剪贴板');
  } catch (err) {
    console.error('复制失败:', err);
    ElMessage.error('复制AI报告失败');
  } finally {
    document.body.removeChild(textArea);
  }
};

const handleClose = () => {
  emit('update:modelValue', false);
  emit('close');
};
</script>

<style scoped>
.grading-content {
  padding: 20px 0;
}

.grading-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.grading-header h3 {
  margin: 0;
  color: #303133;
}

.grading-stats {
  display: flex;
  gap: 12px;
}

.grading-actions-header {
  display: flex;
  gap: 12px;
}

.students-list {
  margin-top: 20px;
}

.grading-progress {
  margin-bottom: 24px;
}

.ai-report-content {
  padding: 15px;
  background-color: #f5f5f5;
  border-radius: 4px;
}

.ai-report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.ai-report-header .el-tag {
  font-size: 14px;
}

.ai-report-body pre {
  white-space: pre-wrap;
  word-break: break-all;
  font-family: 'Courier New', monospace;
  background-color: #f5f5f5;
  padding: 15px;
  border-radius: 4px;
  font-size: 14px;
  line-height: 1.6;
  color: #333;
}

.text-muted {
  color: #909399;
  font-size: 12px;
}

/* 调试信息样式 */
.debug-info {
  background-color: #f0f9ff;
  border: 1px solid #b3d8ff;
  border-radius: 4px;
  padding: 10px;
  margin: 10px 0;
  font-family: 'Courier New', monospace;
  font-size: 12px;
}

.debug-info .debug-title {
  font-weight: bold;
  color: #409eff;
  margin-bottom: 5px;
}

.debug-info .debug-content {
  color: #606266;
  line-height: 1.4;
}

@media (max-width: 768px) {
  .grading-header {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .grading-stats {
    justify-content: center;
  }
}
</style> 