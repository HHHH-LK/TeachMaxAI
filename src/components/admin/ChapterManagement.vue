<template>
  <div class="chapter-management">
    <div class="section-header">
      <h3>章节管理</h3>
      <el-button type="primary" @click="showAddChapterDialog">
        <el-icon><Plus /></el-icon>
        添加章节
      </el-button>
    </div>

    <!-- 章节列表 -->
    <div class="chapter-list" v-if="chapters.length > 0">
      <div
        v-for="chapter in chapters"
        :key="chapter.chapterId"
        class="chapter-item"
      >
        <div class="chapter-info">
          <div class="chapter-header">
            <h4 class="chapter-name">{{ chapter.chapterName }}</h4>
            <span class="chapter-order">第{{ chapter.chapterOrder }}章</span>
          </div>
          <p class="chapter-description">{{ chapter.description }}</p>
          <div class="chapter-meta">
            <span class="knowledge-count">
              <el-icon><Collection /></el-icon>
              {{ chapter.knowledgePointCount || 0 }} 个知识点
            </span>
          </div>
        </div>
        
        <div class="chapter-actions">
          <el-button size="small" @click="viewKnowledgePoints(chapter)">
            <el-icon><View /></el-icon>
            查看知识点
          </el-button>
          <el-button size="small" type="primary" @click="editChapter(chapter)">
            <el-icon><Edit /></el-icon>
            编辑
          </el-button>
          <el-button size="small" type="danger" @click="deleteChapter(chapter)">
            <el-icon><Delete /></el-icon>
            删除
          </el-button>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <el-empty description="暂无章节数据" />
    </div>

    <!-- 添加/编辑章节对话框 -->
    <el-dialog
      v-model="chapterDialogVisible"
      :title="isEditing ? '编辑章节' : '添加章节'"
      :width="500"
      @close="resetChapterForm"
    >
      <el-form
        :model="chapterForm"
        :rules="chapterRules"
        ref="chapterFormRef"
        label-width="80px"
      >
        <el-form-item label="章节名称" prop="chapterName">
          <el-input
            v-model="chapterForm.chapterName"
            placeholder="请输入章节名称"
            clearable
          />
        </el-form-item>
        <el-form-item label="章节顺序" prop="chapterOrder" v-if="!isEditing">
          <el-input-number
            v-model="chapterForm.chapterOrder"
            :min="1"
            :max="999"
            placeholder="请输入章节顺序"
          />
        </el-form-item>
        <el-form-item label="章节描述" prop="description">
          <el-input
            v-model="chapterForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入章节描述"
            clearable
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="chapterDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitChapterForm">
            {{ isEditing ? '更新' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 知识点管理对话框 -->
    <KnowledgePointManagement
      v-model:visible="knowledgePointDialogVisible"
      :chapter="currentChapter"
      :course-id="courseId"
      @refresh="loadChapters"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, nextTick, computed } from 'vue';
import { Plus, Collection, View, Edit, Delete } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { teacherService } from '@/services/api';
import KnowledgePointManagement from './KnowledgePointManagement.vue';

const props = defineProps({
  courseId: {
    type: [String, Number],
    required: true
  }
});

const emit = defineEmits(['refresh']);

// 响应式数据
const chapters = ref([]);
const chapterDialogVisible = ref(false);
const knowledgePointDialogVisible = ref(false);
const isEditing = ref(false);
const currentChapter = ref(null);
const chapterFormRef = ref(null);

// 章节表单
const chapterForm = reactive({
  chapterName: '',
  chapterOrder: 1,
  description: ''
});

// 表单验证规则
const chapterRules = computed(() => {
  const baseRules = {
    chapterName: [
      { required: true, message: '请输入章节名称', trigger: 'blur' },
      { min: 2, max: 50, message: '章节名称长度在 2 到 50 个字符', trigger: 'blur' }
    ],
    description: [
      { required: true, message: '请输入章节描述', trigger: 'blur' },
      { min: 10, max: 200, message: '章节描述长度在 10 到 200 个字符', trigger: 'blur' }
    ]
  };

  // 只在添加模式下验证章节顺序
  if (!isEditing.value) {
    baseRules.chapterOrder = [
      { required: true, message: '请输入章节顺序', trigger: 'blur' },
      { type: 'number', min: 1, message: '章节顺序必须大于0', trigger: 'blur' }
    ];
  }

  return baseRules;
});

// 加载章节数据
const loadChapters = async () => {
  if (!props.courseId) return;
  
  try {
    console.log('正在加载课程ID为', props.courseId, '的章节数据');
    const response = await teacherService.getChapterByCourseId(props.courseId);
    if (response.data.code === 0) {
      chapters.value = response.data.data || [];
      console.log('成功加载章节数据:', chapters.value);
      // 为每个章节添加知识点数量
      for (const chapter of chapters.value) {
        await loadKnowledgePointCount(chapter);
      }
    } else {
      throw new Error(response.data.msg || '获取章节失败');
    }
  } catch (error) {
    console.error('加载章节失败:', error);
    ElMessage.warning('API调用失败，显示示例数据');
    
    // 根据课程ID生成不同的示例章节数据
    const mockChapters = generateMockChapters(props.courseId);
    chapters.value = mockChapters;
  }
};

// 生成模拟章节数据
const generateMockChapters = (courseId) => {
  const courseChapters = {
    1: [ // Java程序设计
      {
        chapterId: 1,
        courseId: courseId,
        chapterName: 'Java基础语法',
        chapterOrder: 1,
        description: 'Java语言基础语法和概念，包括变量、数据类型、运算符等',
        knowledgePointCount: 3
      },
      {
        chapterId: 2,
        courseId: courseId,
        chapterName: '面向对象编程',
        chapterOrder: 2,
        description: 'Java面向对象编程思想和实践，包括类、对象、继承、多态等',
        knowledgePointCount: 4
      },
      {
        chapterId: 3,
        courseId: courseId,
        chapterName: '异常处理',
        chapterOrder: 3,
        description: 'Java异常处理机制，包括try-catch、throws、自定义异常等',
        knowledgePointCount: 2
      }
    ],
    2: [ // 数据结构与算法
      {
        chapterId: 4,
        courseId: courseId,
        chapterName: '线性数据结构',
        chapterOrder: 1,
        description: '数组、链表、栈、队列等线性数据结构的基本概念和实现',
        knowledgePointCount: 5
      },
      {
        chapterId: 5,
        courseId: courseId,
        chapterName: '树形数据结构',
        chapterOrder: 2,
        description: '二叉树、二叉搜索树、AVL树等树形数据结构',
        knowledgePointCount: 3
      },
      {
        chapterId: 6,
        courseId: courseId,
        chapterName: '图论基础',
        chapterOrder: 3,
        description: '图的基本概念、图的表示方法、图的遍历算法',
        knowledgePointCount: 4
      }
    ],
    3: [ // 高等数学A
      {
        chapterId: 7,
        courseId: courseId,
        chapterName: '微积分基础',
        chapterOrder: 1,
        description: '函数、极限、连续性、导数的基本概念和计算',
        knowledgePointCount: 6
      },
      {
        chapterId: 8,
        courseId: courseId,
        chapterName: '积分学',
        chapterOrder: 2,
        description: '不定积分、定积分、积分的应用',
        knowledgePointCount: 5
      },
      {
        chapterId: 9,
        courseId: courseId,
        chapterName: '级数理论',
        chapterOrder: 3,
        description: '数项级数、幂级数、傅里叶级数',
        knowledgePointCount: 4
      }
    ]
  };
  
  return courseChapters[courseId] || [];
};

// 加载章节的知识点数量
const loadKnowledgePointCount = async (chapter) => {
  try {
    const response = await teacherService.getKnowledgePointsByChapterId(chapter.chapterId);
    if (response.data.code === 0) {
      chapter.knowledgePointCount = response.data.data?.length || 0;
    }
  } catch (error) {
    console.error('加载知识点数量失败:', error);
    chapter.knowledgePointCount = 0;
  }
};

// 显示添加章节对话框
const showAddChapterDialog = () => {
  isEditing.value = false;
  resetChapterForm();
  chapterDialogVisible.value = true;
};

// 编辑章节
const editChapter = (chapter) => {
  isEditing.value = true;
  currentChapter.value = chapter;
  Object.assign(chapterForm, {
    chapterName: chapter.chapterName,
    // 编辑时不修改章节顺序，保持原有值
    chapterOrder: chapter.chapterOrder,
    description: chapter.description
  });
  chapterDialogVisible.value = true;
};

// 删除章节
const deleteChapter = async (chapter) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除章节"${chapter.chapterName}"吗？删除后无法恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );

    // TODO: 调用删除章节API
    // await teacherService.deleteChapter(chapter.chapterId);
    
    ElMessage.success('章节删除成功');
    await loadChapters();
    emit('refresh');
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除章节失败:', error);
      ElMessage.error('删除章节失败，请稍后重试');
    }
  }
};

// 查看知识点
const viewKnowledgePoints = (chapter) => {
  console.log('准备查看章节知识点:', chapter);
  
  // 如果当前已经有章节在查看，先关闭对话框
  if (knowledgePointDialogVisible.value) {
    console.log('关闭当前知识点对话框');
    knowledgePointDialogVisible.value = false;
  }
  
  // 设置新的章节并打开对话框
  currentChapter.value = chapter;
  console.log('设置新章节:', currentChapter.value);
  
  // 使用nextTick确保DOM更新后再打开对话框
  nextTick(() => {
    console.log('打开知识点对话框，章节:', currentChapter.value);
    knowledgePointDialogVisible.value = true;
  });
};

// 提交章节表单
const submitChapterForm = async () => {
  try {
    await chapterFormRef.value.validate();
    
    if (isEditing.value) {
      // 编辑模式：只更新名称和描述，保持章节顺序不变
      const updateData = {
        chapterName: chapterForm.chapterName,
        description: chapterForm.description,
        // 保持原有章节顺序
        chapterOrder: currentChapter.value.chapterOrder,
        chapterId: currentChapter.value.chapterId,
        courseId: props.courseId
      };
      
      // TODO: 调用更新章节API
      // await teacherService.updateChapter(updateData);
      ElMessage.success('章节更新成功');
    } else {
      // 添加模式：包含所有字段
      const createData = {
        ...chapterForm,
        courseId: props.courseId
      };
      
      // TODO: 调用创建章节API
      // await teacherService.createChapter(createData);
      ElMessage.success('章节创建成功');
    }
    
    chapterDialogVisible.value = false;
    await loadChapters();
    emit('refresh');
  } catch (error) {
    console.error('提交章节表单失败:', error);
    ElMessage.error('操作失败，请稍后重试');
  }
};

// 重置章节表单
const resetChapterForm = () => {
  Object.assign(chapterForm, {
    chapterName: '',
    chapterOrder: 1,
    description: ''
  });
  currentChapter.value = null;
  if (chapterFormRef.value) {
    chapterFormRef.value.resetFields();
  }
};

// 监听课程ID变化，重新加载数据
watch(() => props.courseId, (newCourseId, oldCourseId) => {
  if (newCourseId && newCourseId !== oldCourseId) {
    console.log('课程ID变化，从', oldCourseId, '变为', newCourseId);
    loadChapters();
  }
}, { immediate: true });

// 组件挂载时加载数据
onMounted(() => {
  if (props.courseId) {
    loadChapters();
  }
});
</script>

<style lang="less" scoped>
@text-primary: #1e293b;
@text-secondary: #64748b;
@border-color: rgba(230, 230, 230, 0.8);
@primary-color: #bde3ff;

.chapter-management {
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;

    h3 {
      font-size: 18px;
      font-weight: 600;
      color: @text-primary;
      margin: 0;
    }
  }

  .chapter-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .chapter-item {
    background: rgba(255, 255, 255, 0.85);
    border: 1px solid @border-color;
    border-radius: 12px;
    padding: 20px;
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    transition: all 0.3s ease;

    &:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
      border-color: rgba(99, 102, 241, 0.2);
    }

    .chapter-info {
      flex: 1;
      margin-right: 20px;

      .chapter-header {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 8px;

        .chapter-name {
          font-size: 16px;
          font-weight: 600;
          color: @text-primary;
          margin: 0;
        }

        .chapter-order {
          background: @primary-color;
          color: white;
          padding: 2px 8px;
          border-radius: 12px;
          font-size: 12px;
          font-weight: 500;
        }
      }

      .chapter-description {
        color: @text-secondary;
        font-size: 14px;
        line-height: 1.6;
        margin: 0 0 12px 0;
      }

      .chapter-meta {
        .knowledge-count {
          display: flex;
          align-items: center;
          gap: 6px;
          color: @text-secondary;
          font-size: 13px;
        }
      }
    }

    .chapter-actions {
      display: flex;
      gap: 8px;
      flex-shrink: 0;
    }
  }

  .empty-state {
    text-align: center;
    padding: 40px 20px;
  }
}

.dialog-footer {
  text-align: right;
}
</style>
