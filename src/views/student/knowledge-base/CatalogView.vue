<template>
  <div class="catalog-view">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>加载课程目录中...</p>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-state">
      <i class="icon-error"></i>
      <h3>加载失败</h3>
      <p>{{ error }}</p>
      <button @click="fetchCourseCatalog">重试</button>
    </div>

    <!-- 正常内容 -->
    <div v-else>
      <div class="content-header">
        <h2 class="content-title">课程目录</h2>
        <div class="content-stats">
          <span class="stat-item">{{ chapters.length }} 章节</span>
          <span class="stat-item">{{ totalKnowledgePoints }} 个知识点</span>
        </div>
      </div>

      <div class="chapter-list">
        <div
          v-for="(chapter, index) in chapters"
          :key="chapter.id"
          class="chapter-item"
        >
          <div class="chapter-header" @click="toggleChapter(index)">
            <div class="chapter-info">
              <div class="chapter-number">
                {{ index + 1 }}
              </div>
              <div class="chapter-details">
                <div class="chapter-title">{{ chapter.title }}</div>
                <div class="chapter-meta">
                  <span>{{ chapter.description }}</span>
                </div>
              </div>
            </div>
            <div class="chapter-actions">
              <div class="progress-container">
                <div class="progress-bar-bg">
                  <div
                    class="progress-bar-fg"
                    :style="{ width: chapter.progress + '%' }"
                  ></div>
                </div>
                <span class="progress-text">{{ chapter.progress }}%</span>
              </div>
              <div class="toggle-icon">
                <i
                  class="icon-arrow"
                  :class="{ rotated: chapter.expanded }"
                ></i>
              </div>
            </div>
          </div>

          <!-- 知识点列表 -->
          <div class="knowledge-points-list" v-if="chapter.expanded">
            <div
              v-for="point in chapter.knowledgePoints"
              :key="point.id"
              class="knowledge-point-item"
              @click="showKnowledgePointDetail(point)"
            >
              <div class="point-info">
                <div class="point-title">
                  <span class="point-name">{{ point.name }}</span>
                  <span v-if="point.isCore" class="core-badge">核心</span>
                </div>
              </div>
              <div class="point-difficulty">
                <span
                  class="difficulty"
                  :class="getDifficultyClass(point.difficulty)"
                >
                  {{ getDifficultyText(point.difficulty) }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 知识点详情弹窗 -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="
        selectedKnowledgePoint ? selectedKnowledgePoint.name : '知识点详情'
      "
      width="600px"
      class="knowledge-point-dialog"
    >
      <div v-if="selectedKnowledgePoint" class="knowledge-point-detail">
        <div class="detail-header">
          <h3 class="detail-title">
            {{ selectedKnowledgePoint.name }}
            <span v-if="selectedKnowledgePoint.isCore" class="core-badge"
              >核心</span
            >
          </h3>
          <div class="detail-meta">
            <span
              class="difficulty"
              :class="getDifficultyClass(selectedKnowledgePoint.difficulty)"
            >
              {{ getDifficultyText(selectedKnowledgePoint.difficulty) }}
            </span>
          </div>
        </div>

        <div class="detail-content">
          <div class="detail-section">
            <h4>知识点描述</h4>
            <p>{{ selectedKnowledgePoint.description || "暂无描述" }}</p>
          </div>

          <div class="detail-section">
            <h4>前置知识</h4>
            <div class="prerequisites">
              <template
                v-if="
                  selectedKnowledgePoint.prerequisites &&
                  selectedKnowledgePoint.prerequisites.length
                "
              >
                <span
                  v-for="pre in selectedKnowledgePoint.prerequisites"
                  :key="pre"
                  class="prerequisite-tag"
                >
                  {{ pre }}
                </span>
              </template>
              <span v-else class="no-prerequisites">无前置知识要求</span>
            </div>
          </div>
        </div>
        <div class="detail-actions">
          <button
            class="start-learning-btn"
            @click="startLearning(selectedKnowledgePoint)"
          >
            开始学习
          </button>
        </div>
      </div>
    </el-dialog>

    <el-dialog
      v-model="learningPreviewVisible"
      :title="`学习：${learningPointName || '知识点'}`"
      width="900px"
      class="learning-preview-dialog"
    >
      <!-- 简化的工具栏，只保留状态提示 -->
      <div class="preview-toolbar">
        <span v-if="frameLoading" class="loading-text">加载中...</span>
        <span v-if="frameError" class="error-text"
          >无法加载内容，可能受浏览器安全限制</span
        >
      </div>

      <div class="preview-container">
        <iframe
          v-if="learningUrl"
          :src="learningUrl"
          class="preview-iframe"
          @load="handleFrameLoad"
          @error="handleFrameError"
          frameborder="0"
        ></iframe>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="learningPreviewVisible = false"
            >退出学习</el-button
          >
          <el-button type="primary" @click="handleLearningFinish"
            >完成学习</el-button
          >
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from "vue";
import { ElMessage } from "element-plus";
import { studentService } from "@/services/api";
import {getCurrentUserId} from "@/utils/userUtils.js";

// 接收父组件传递的courseId
const props = defineProps({
  courseId: {
    type: Number,
    required: true,
  },
});

// 学习预览相关变量
const learningPreviewVisible = ref(false);
const learningUrl = ref("");
const learningPointName = ref("");
const frameLoading = ref(false);
const frameError = ref(false);

// 组件状态
const loading = ref(false);
const error = ref(null);
const chapters = ref([]);

// 知识点详情弹窗
const detailDialogVisible = ref(false);
const selectedKnowledgePoint = ref(null);

// 学习记录相关变量
const currentLearningPoint = ref(null);
const learningStartTime = ref(null);
const learningTimer = ref(null);
const elapsedTime = ref(0); // 学习时长（秒）

// 计算总知识点数量
const totalKnowledgePoints = computed(() => {
  return chapters.value.reduce((total, chapter) => {
    return total + (chapter.knowledgePoints?.length || 0);
  }, 0);
});

// 切换章节展开/折叠状态
const toggleChapter = (index) => {
  chapters.value[index].expanded = !chapters.value[index].expanded;
};

// 显示知识点详情
const showKnowledgePointDetail = (knowledgePoint) => {
  selectedKnowledgePoint.value = knowledgePoint;
  detailDialogVisible.value = true;
};

// 开始学习
const startLearning = async (point) => {
  try {
    // 记录当前学习知识点
    currentLearningPoint.value = point;

    // 记录开始时间
    learningStartTime.value = new Date();
    elapsedTime.value = 0;

    // 调用开始学习API
    // const response = await studentService.startStudy({
    //   chapterId: point.chapterId.toString(),
    //   nowmaterialId: point.id.toString(),
    //   studyTime: new Date().toISOString(),
    //   courseId: props.courseId.toString()
    // });
    //
    // if (response.data.code === 0) {
    //   ElMessage.success("学习已开始，系统将自动记录学习时间");
    // } else {
    //   ElMessage.warning(response.data.msg || "学习开始记录失败");
    // }

    const userId = getCurrentUserId();
    const res = await studentService.getStudentIdByUserId(userId)
    const studentId = res.data.data.studentId;

    const url = await studentService.getStudyByAgent(studentId, point.id)
    const originalUrl = url.data.data;
    const pointId = originalUrl.replace(/^["']|["']$/g, '');
    // console.log("获取到url", pointId)

    // 设置学习链接和知识点名称
    learningUrl.value = pointId;
    learningPointName.value = point.name;

    // 重置加载状态
    frameLoading.value = true;
    frameError.value = false;

    // 显示预览框并关闭详情弹窗
    learningPreviewVisible.value = true;
    detailDialogVisible.value = false;

    // 启动计时器
    startLearningTimer();
  } catch (err) {
    console.error("开始学习失败:", err);
    ElMessage.error("开始学习失败，请重试");
  }
};

// 启动学习计时器
const startLearningTimer = () => {
  learningTimer.value = setInterval(() => {
    elapsedTime.value++;
  }, 1000);
};

// 停止学习计时器
const stopLearningTimer = () => {
  if (learningTimer.value) {
    clearInterval(learningTimer.value);
    learningTimer.value = null;
  }
};

// 处理学习结束
const handleLearningEnd = async () => {
  try {
    // 停止计时器
    stopLearningTimer();

    if (!currentLearningPoint.value) return;

    // 调用结束学习API
    const response = await studentService.endStudy({
      chapterId: currentLearningPoint.value.chapterId.toString(),
      nowmaterialId: currentLearningPoint.value.id.toString(),
      studyTime: formatTimeForBackend(),
      courseId: props.courseId.toString()
    });

    if (response.data.code === 0) {
      ElMessage.success(
        `学习结束，本次学习时长: ${formatTime(elapsedTime.value)}`
      );
    } else {
      ElMessage.warning(response.data.msg || "学习结束记录失败");
    }

    // 重置学习状态
    currentLearningPoint.value = null;
    learningStartTime.value = null;
    elapsedTime.value = 0;
  } catch (err) {
    console.error("结束学习失败:", err);
    ElMessage.error("结束学习失败，请重试");
  }
};

// 创建格式化时间函数
const formatTimeForBackend = () => {
  const now = new Date();
  return `${now.getFullYear()}-${(now.getMonth() + 1).toString().padStart(2, '0')}-${now.getDate().toString().padStart(2, '0')} ` +
         `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`;
};

// 处理学习完成
const handleLearningFinish = async () => {
  try {
    // 停止计时器
    stopLearningTimer();

    if (!currentLearningPoint.value) return;

    // 调用完成学习API
    const response = await studentService.finishStudy({
      chapterId: currentLearningPoint.value.chapterId.toString(),
      nowmaterialId: currentLearningPoint.value.id.toString(),
      studyTime: formatTimeForBackend(), // 完成时间
      courseId: props.courseId.toString()
    });

    

    if (response.data.code === 0) {
      ElMessage.success(`学习完成！总时长: ${formatTime(elapsedTime.value)}`);
      // 刷新课程目录以更新进度
      await fetchCourseCatalog();
    } else {
      ElMessage.warning(response.data.msg || "学习完成记录失败");
    }

    // 重置学习状态
    currentLearningPoint.value = null;
    learningStartTime.value = null;
    elapsedTime.value = 0;
  } catch (err) {
    console.error("完成学习失败:", err);
    ElMessage.error("完成学习失败，请重试");
  }
};

// 格式化时间（秒 -> 分:秒）
const formatTime = (seconds) => {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins.toString().padStart(2, "0")}:${secs
    .toString()
    .padStart(2, "0")}`;
};

const handleFrameLoad = () => {
  frameLoading.value = false;
};

const handleFrameError = () => {
  frameLoading.value = false;
  frameError.value = true;
};

// 获取难度等级样式类
const getDifficultyClass = (difficulty) => {
  const classes = {
    easy: "difficulty-easy",
    medium: "difficulty-medium",
    hard: "difficulty-hard",
  };
  return classes[difficulty] || "difficulty-medium";
};

// 获取难度等级文本
const getDifficultyText = (difficulty) => {
  const texts = {
    easy: "简单",
    medium: "中等",
    hard: "困难",
  };
  return texts[difficulty] || "中等";
};

// 获取课程目录数据
const fetchCourseCatalog = async () => {
  const allKnowledge = [];
  try {
    loading.value = true;
    error.value = null;

    // 调用API获取课程目录
    const responseChapter = await studentService.getChapterInfo(props.courseId);
    if (responseChapter.data) {
      chapters.value = responseChapter.data.data.map((chapter) => ({
        id: chapter.chapterId,
        title: chapter.chapterName,
        description: chapter.description,
        progress: chapter.progressRate,
        expanded: false,
        knowledgePoints: [],
      }));
    }

    // 获取每个章节的知识点
    for (let i = 0; i < chapters.value.length; i++) {
      const responseKnw = await studentService.getChapterKnow(
        chapters.value[i].id
      );
      if (responseKnw.data) {
        const chapterKnowledge = responseKnw.data.data.map((knowledge) => ({
          id: knowledge.pointId,
          chapterId: chapters.value[i].id,
          name: knowledge.pointName,
          description: knowledge.description,
          difficulty: knowledge.difficultyLevel,
          isCore: knowledge.isCore,
          objectives: knowledge.keywords,
          prerequisites: ["无"],
        }));
        chapters.value[i].knowledgePoints = chapterKnowledge;
        allKnowledge.push(...chapterKnowledge);
      }
    }
  } catch (err) {
    console.error("获取课程目录失败:", err);
    error.value = "加载课程目录失败";
  } finally {
    loading.value = false;
  }
};

// 监听courseId的变化
watch(
  () => props.courseId,
  (newCourseId) => {
    if (newCourseId) {
      fetchCourseCatalog();
    }
  },
  { immediate: true }
);

// 监听学习预览弹窗状态
watch(learningPreviewVisible, (newVal) => {
  if (!newVal) {
    // 弹窗关闭时处理学习结束
    handleLearningEnd();
  }
});
</script>

<style lang="less" scoped>
@primary: #2563eb; /* 主蓝色 */
@primary-light: #3b82f6; /* 浅一点的蓝色 */
@primary-dark: #1d4ed8; /* 深一点的蓝色 */
@primary-bg: #eff6ff; /* 蓝色背景 */
@primary-border: #dbeafe; /* 蓝色边框 */
@secondary: #60a5fa; /* 辅助蓝色 */
@success: #10b981; /* 成功色（搭配蓝色） */
@warning: #f59e0b; /* 警告色 */
@danger: #ef4444; /* 危险色 */
@text-primary: #1e293b; /* 主要文字 */
@text-secondary: #64748b; /* 次要文字 */
@text-tertiary: #94a3b8; /* tertiary文字 */
@bg-white: #ffffff; /* 白色背景 */
@bg-light: #f8fafc; /* 浅色背景 */
@border-light: #e2e8f0; /* 浅色边框 */
@shadow-sm: 0 2px 8px rgba(37, 99, 235, 0.1); /* 小阴影 */
@shadow-md: 0 4px 16px rgba(37, 99, 235, 0.15); /* 中阴影 */
@radius-sm: 6px;
@radius-md: 12px;
@radius-lg: 16px;
@transition: all 0.3s ease;
@breakpoint-md: 1024px;
@breakpoint-sm: 768px;

.catalog-view {
  padding: 20px;
  background-color: @bg-white;
  border-radius: @radius-md;
  box-shadow: @shadow-sm;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;

  .content-title {
    font-size: 22px;
    font-weight: 600;
    color: @text-primary;
    margin: 0;
    display: flex;
    align-items: center;
    gap: 8px;

    &::before {
      content: "";
      width: 4px;
      height: 20px;
      background-color: @primary;
      border-radius: 2px;
    }
  }

  .content-stats {
    display: flex;
    align-items: center;
    gap: 20px;
    color: @text-tertiary;
    font-size: 13px;

    .stat-item {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }
}

.chapter-list {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .chapter-item {
    background-color: @bg-light;
    border-radius: @radius-md;
    border: 1px solid @border-light;
    overflow: hidden;
    transition: @transition;

    &:hover {
      border-color: @primary-border;
      box-shadow: @shadow-sm;
    }

    .chapter-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 18px 24px;
      cursor: pointer;
      transition: @transition;

      &:hover {
        background-color: @primary-bg;

        .chapter-title {
          color: @primary;
        }

        .toggle-icon {
          color: @primary;
        }
      }

      .chapter-info {
        display: flex;
        align-items: center;
        gap: 16px;

        .chapter-number {
          display: flex;
          align-items: center;
          justify-content: center;
          width: 32px;
          height: 32px;
          background-color: @primary;
          color: white;
          border-radius: 6px;
          font-weight: 600;
          font-size: 14px;
        }

        .chapter-details {
          display: flex;
          flex-direction: column;
          gap: 4px;

          .chapter-title {
            font-size: 16px;
            font-weight: 600;
            color: @text-primary;
            transition: @transition;
          }

          .chapter-meta {
            display: flex;
            align-items: center;
            gap: 8px;
            color: @text-tertiary;
            font-size: 13px;
          }
        }
      }

      .chapter-actions {
        display: flex;
        align-items: center;
        gap: 16px;

        .progress-container {
          display: flex;
          align-items: center;
          gap: 8px;
          width: 120px;
        }
        .progress-bar-bg {
          position: relative;
          flex: 1;
          height: 6px;
          background-color: @border-light;
          border-radius: 3px;
          overflow: hidden;
        }
        .progress-bar-fg {
          position: absolute;
          left: 0;
          top: 0;
          height: 100%;
          background-color: @success;
          border-radius: 3px;
          transition: width 0.6s ease;
        }
        .progress-text {
          font-size: 13px;
          font-weight: 500;
          color: @text-secondary;
          min-width: 36px;
          text-align: right;
        }

        .toggle-icon {
          color: @text-tertiary;
          transition: @transition;
        }
      }
    }

    .knowledge-points-list {
      border-top: 1px solid @border-light;
      background-color: @bg-white;

      .knowledge-point-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 16px 24px;
        border-bottom: 1px solid @border-light;
        cursor: pointer;
        transition: @transition;

        &:last-child {
          border-bottom: none;
        }

        &:hover {
          background-color: @primary-bg;

          .point-name {
            color: @primary;
          }
        }

        .point-info {
          flex: 1;
          display: flex;
          flex-direction: column;
          gap: 6px;

          .point-title {
            display: flex;
            align-items: center;
            gap: 8px;

            .point-name {
              font-size: 14px;
              font-weight: 500;
              color: @text-primary;
              transition: @transition;
            }

            .core-badge {
              background-color: @warning;
              color: white;
              font-size: 10px;
              padding: 2px 6px;
              border-radius: 4px;
              font-weight: 600;
            }
          }
        }

        .point-difficulty {
          display: flex;
          align-items: center;
          justify-content: flex-end;
          min-width: 60px;

          .difficulty {
            padding: 4px 8px;
            border-radius: 4px;
            font-weight: 500;
            font-size: 12px;

            &.difficulty-easy {
              background-color: #dcfce7;
              color: #166534;
            }

            &.difficulty-medium {
              background-color: #fef3c7;
              color: #92400e;
            }

            &.difficulty-hard {
              background-color: #fee2e2;
              color: #991b1b;
            }
          }
        }
      }
    }
  }
}

.icon-arrow {
  display: inline-block;
  width: 16px;
  height: 16px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");
  transition: transform 0.3s ease;

  &.rotated {
    transform: rotate(180deg);
  }
}

// 知识点详情弹窗样式
:deep(.knowledge-point-dialog) {
  .el-dialog__header {
    border-bottom: 1px solid @border-light;
    padding: 20px 24px;
  }

  .el-dialog__body {
    padding: 24px;
  }
}

.knowledge-point-detail {
  .detail-header {
    margin-bottom: 24px;
    padding-bottom: 16px;
    border-bottom: 1px solid @border-light;

    .detail-title {
      font-size: 20px;
      font-weight: 600;
      color: @text-primary;
      margin: 0 0 12px 0;
      display: flex;
      align-items: center;
      gap: 8px;

      .core-badge {
        background-color: @warning;
        color: white;
        font-size: 12px;
        padding: 4px 8px;
        border-radius: 4px;
        font-weight: 600;
      }
    }

    .detail-meta {
      display: flex;
      align-items: center;
      gap: 16px;

      .difficulty {
        padding: 4px 8px;
        border-radius: 4px;
        font-weight: 500;
        font-size: 12px;

        &.difficulty-easy {
          background-color: #dcfce7;
          color: #166534;
        }

        &.difficulty-medium {
          background-color: #fef3c7;
          color: #92400e;
        }

        &.difficulty-hard {
          background-color: #fee2e2;
          color: #991b1b;
        }
      }
    }
  }

  .detail-content {
    margin-bottom: 24px;

    .detail-section {
      margin-bottom: 20px;

      h4 {
        font-size: 16px;
        font-weight: 600;
        color: @text-primary;
        margin: 0 0 12px 0;
      }

      p {
        color: @text-secondary;
        line-height: 1.6;
        margin: 0;
      }

      ul {
        margin: 0;
        padding-left: 20px;
        color: @text-secondary;
        line-height: 1.6;

        li {
          margin-bottom: 8px;
        }
      }

      .prerequisites {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;

        .prerequisite-tag {
          background-color: @primary-bg;
          color: @primary;
          padding: 4px 8px;
          border-radius: 4px;
          font-size: 12px;
          font-weight: 500;
        }
      }
    }
  }
}

@media (max-width: @breakpoint-md) {
  .catalog-view {
    .content-header {
      flex-direction: column;
      align-items: flex-start;
      gap: 12px;
    }

    .chapter-list {
      .chapter-item {
        .chapter-header {
          flex-direction: column;
          align-items: flex-start;
          gap: 12px;

          .chapter-actions {
            width: 100%;
            justify-content: space-between;

            .progress-container {
              width: auto;
              flex: 1;
            }
          }
        }

        .knowledge-points-list {
          .knowledge-point-item {
            flex-direction: column;
            align-items: flex-start;
            gap: 12px;

            .point-status {
              width: 100%;
              justify-content: space-between;

              .point-progress {
                flex: 1;
              }
            }
          }
        }
      }
    }
  }
}

@media (max-width: @breakpoint-sm) {
  .catalog-view {
    padding: 16px;

    .content-header {
      flex-direction: column;
      align-items: flex-start;
      gap: 12px;
    }

    .chapter-list {
      .chapter-item {
        .chapter-header {
          flex-direction: column;
          align-items: flex-start;
          gap: 12px;

          .chapter-actions {
            width: 100%;
            justify-content: space-between;

            .progress-container {
              width: auto;
              flex: 1;
            }
          }
        }

        .knowledge-points-list {
          .knowledge-point-item {
            flex-direction: column;
            align-items: flex-start;
            gap: 12px;

            .point-status {
              width: 100%;
              justify-content: space-between;

              .point-progress {
                flex: 1;
              }
            }
          }
        }
      }
    }
  }
}
.detail-actions {
  margin-top: 20px;
  text-align: right;
}

.start-learning-btn {
  background-color: @primary;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 5px;
  cursor: pointer;
  font-size: 16px;
  transition: background-color 0.3s ease;
}

.start-learning-btn:hover {
  background-color: @primary-dark;
}

.learning-preview-dialog {
  .el-dialog__body {
    padding: 0;
    height: calc(80vh);
    overflow: hidden;
  }

  .preview-toolbar {
    padding: 12px 16px;
    border-bottom: 1px solid @border-light;
    display: flex;
    align-items: center;
    gap: 12px;
    background-color: @bg-light;
  }

  .preview-container {
    width: 100%;
    height: calc(100% - 55px); /* 减去工具栏高度 */
    overflow: hidden;
  }

  .preview-iframe {
    width: 100%;
    height: 100%;
    min-height: 600px;
  }

  .loading-text {
    color: @text-tertiary;
    font-size: 13px;
    margin-left: auto;
  }

  .error-text {
    color: @danger;
    font-size: 13px;
    margin-left: auto;
  }
}

// 适配全屏样式
:deep(.el-dialog--fullscreen) {
  .preview-container {
    height: calc(100vh - 100px);
  }
}
</style>
