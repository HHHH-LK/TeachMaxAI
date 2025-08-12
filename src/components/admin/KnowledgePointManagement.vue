<template>
  <el-dialog
    v-model="dialogVisible"
    :title="`${chapter?.chapterName || '章节'} - 知识点管理`"
    :width="900"
    append-to-body
    center
    lock-scroll
    modal
    :close-on-press-escape="false"
    @close="handleClose"
  >
    <div class="knowledge-point-management">
      <div class="section-header">
        <h4>知识点列表</h4>
        <el-button type="primary" @click="showAddKnowledgePointDialog">
          <el-icon><Plus /></el-icon>
          添加知识点
        </el-button>
      </div>

      <!-- 知识点列表 -->
      <div class="knowledge-point-list" v-if="knowledgePoints.length > 0">
        <div
          v-for="point in knowledgePoints"
          :key="point.pointId"
          class="knowledge-point-item"
        >
          <div class="point-info">
            <div class="point-header">
              <h5 class="point-name">{{ point.pointName }}</h5>
              <el-tag
                :type="getDifficultyType(point.difficultyLevel)"
                size="small"
              >
                {{ getDifficultyText(point.difficultyLevel) }}
              </el-tag>
            </div>
            <p class="point-description">{{ point.description }}</p>
            <div class="point-meta">
              <span class="keywords">
                <el-icon><Collection /></el-icon>
                关键词: {{ point.keywords || '无' }}
              </span>
            </div>
          </div>
          
          <div class="point-actions">
            <el-button size="small" type="primary" @click="editKnowledgePoint(point)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button size="small" type="danger" @click="deleteKnowledgePoint(point)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <el-empty description="暂无知识点数据" />
      </div>
    </div>

    <!-- 添加/编辑知识点对话框 -->
    <el-dialog
      v-model="knowledgePointDialogVisible"
      :title="isEditing ? '编辑知识点' : '添加知识点'"
      :width="600"
      append-to-body
      center
      lock-scroll
      modal
      :close-on-press-escape="false"
      @close="resetKnowledgePointForm"
    >
      <el-form
        :model="knowledgePointForm"
        :rules="knowledgePointRules"
        ref="knowledgePointFormRef"
        label-width="100px"
      >
        <el-form-item label="知识点名称" prop="pointName">
          <el-input
            v-model="knowledgePointForm.pointName"
            placeholder="请输入知识点名称"
            clearable
          />
        </el-form-item>
        <el-form-item label="难度等级" prop="difficultyLevel">
          <el-select
            v-model="knowledgePointForm.difficultyLevel"
            placeholder="请选择难度等级"
            clearable
          >
            <el-option label="简单" value="easy" />
            <el-option label="中等" value="medium" />
            <el-option label="困难" value="hard" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识点描述" prop="description">
          <el-input
            v-model="knowledgePointForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入知识点描述"
            clearable
          />
        </el-form-item>
        <el-form-item label="关键词" prop="keywords">
          <el-input
            v-model="knowledgePointForm.keywords"
            placeholder="请输入关键词，用逗号分隔"
            clearable
          />
          <div class="form-tip">多个关键词请用逗号分隔，如：变量,数据类型,运算符</div>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="knowledgePointDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitKnowledgePointForm">
            {{ isEditing ? '更新' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue';
import { Plus, Collection, Edit, Delete } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { teacherService, adminService } from '@/services/api';

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  chapter: {
    type: Object,
    default: null
  },
  courseId: {
    type: [String, Number],
    required: true
  }
});

const emit = defineEmits(['update:visible', 'refresh']);

// 响应式数据列表
const dialogVisible = ref(false);
const knowledgePoints = ref([]);
const knowledgePointDialogVisible = ref(false);
const isEditing = ref(false);
const currentKnowledgePoint = ref(null);
const knowledgePointFormRef = ref(null);

// 知识点表单
const knowledgePointForm = reactive({
  pointName: '',
  difficultyLevel: 'easy',
  description: '',
  keywords: ''
});

// 表单验证规则
const knowledgePointRules = {
  pointName: [
    { required: true, message: '请输入知识点名称', trigger: 'blur' },
    { min: 2, max: 50, message: '知识点名称长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  difficultyLevel: [
    { required: true, message: '请选择难度等级', trigger: 'change' }
  ],
  description: [
    { required: true, message: '请输入知识点描述', trigger: 'blur' },
    { min: 10, max: 200, message: '知识点描述长度在 10 到 200 个字符', trigger: 'blur' }
  ],
  keywords: [
    { required: false, message: '请输入关键词', trigger: 'blur' }
  ]
};

// 监听visible属性变化
watch(() => props.visible, (newVal) => {
  console.log('知识点管理对话框visible变化:', newVal, '章节:', props.chapter);
  dialogVisible.value = newVal;
  if (newVal && props.chapter) {
    console.log('知识点管理对话框打开，章节:', props.chapter);
    loadKnowledgePoints();
  }
});

// 监听章节变化，重新加载知识点
watch(() => props.chapter, (newChapter, oldChapter) => {
  if (newChapter && newChapter.chapterId !== oldChapter?.chapterId) {
    console.log('章节变化，从', oldChapter?.chapterId, '变为', newChapter.chapterId);
    // 无论对话框是否打开，都重新加载知识点
    loadKnowledgePoints();
  }
}, { immediate: true, deep: true });

// 监听对话框状态变化
watch(dialogVisible, (newVal) => {
  if (!newVal) {
    emit('update:visible', false);
  }
});

// 加载知识点数据
const loadKnowledgePoints = async () => {
  if (!props.chapter?.chapterId) {
    console.log('章节ID不存在，无法加载知识点');
    knowledgePoints.value = [];
    return;
  }
  
  try {
    console.log('正在加载章节ID为', props.chapter.chapterId, '的知识点数据');
    const response = await teacherService.getKnowledgePointsByChapterId(props.chapter.chapterId);
    if (response.data.code === 0) {
      knowledgePoints.value = response.data.data || [];
      console.log('成功加载知识点数据:', knowledgePoints.value);
    } else {
      throw new Error(response.data.msg || '获取知识点失败');
    }
  } catch (error) {
    console.error('加载知识点失败:', error);
    ElMessage.warning('API调用失败，显示示例数据');
    
    // 根据章节ID生成不同的示例知识点数据
    const mockKnowledgePoints = generateMockKnowledgePoints(props.chapter.chapterId);
    knowledgePoints.value = mockKnowledgePoints;
  }
};

// 生成模拟知识点数据
const generateMockKnowledgePoints = (chapterId) => {
  const chapterKnowledgePoints = {
    1: [ // Java基础语法
      {
        pointId: 1,
        pointName: '变量和数据类型',
        description: 'Java中的基本数据类型和变量声明，包括int、double、String等',
        difficultyLevel: 'easy',
        keywords: 'int,double,String,变量,数据类型'
      },
      {
        pointId: 2,
        pointName: '运算符',
        description: 'Java中的各种运算符使用，包括算术运算符、逻辑运算符、比较运算符',
        difficultyLevel: 'easy',
        keywords: '算术运算符,逻辑运算符,比较运算符'
      },
      {
        pointId: 3,
        pointName: '控制结构',
        description: 'if-else、for、while等控制语句的使用方法和注意事项',
        difficultyLevel: 'easy',
        keywords: 'if,for,while,循环,条件'
      }
    ],
    2: [ // 面向对象编程
      {
        pointId: 4,
        pointName: '类和对象',
        description: 'Java中类和对象的概念，包括类的定义、对象的创建和使用',
        difficultyLevel: 'medium',
        keywords: 'class,object,实例化,构造方法'
      },
      {
        pointId: 5,
        pointName: '继承和多态',
        description: '面向对象编程中的继承机制和多态性的实现',
        difficultyLevel: 'medium',
        keywords: 'extends,super,override,多态'
      },
      {
        pointId: 6,
        pointName: '接口和抽象类',
        description: '接口和抽象类的定义、实现和使用场景',
        difficultyLevel: 'hard',
        keywords: 'interface,abstract,implements'
      },
      {
        pointId: 7,
        pointName: '封装和访问控制',
        description: 'Java中的封装概念和访问控制修饰符的使用',
        difficultyLevel: 'medium',
        keywords: 'private,public,protected,封装'
      }
    ],
    3: [ // 异常处理
      {
        pointId: 8,
        pointName: '异常类型',
        description: 'Java中异常的分类，包括检查异常和运行时异常',
        difficultyLevel: 'medium',
        keywords: 'Exception,RuntimeException,检查异常'
      },
      {
        pointId: 9,
        pointName: '异常处理机制',
        description: 'try-catch-finally语句的使用和异常处理的最佳实践',
        difficultyLevel: 'medium',
        keywords: 'try,catch,finally,异常处理'
      }
    ]
  };
  
  return chapterKnowledgePoints[chapterId] || [];
};

// 显示添加知识点对话框
const showAddKnowledgePointDialog = () => {
  isEditing.value = false;
  resetKnowledgePointForm();
  knowledgePointDialogVisible.value = true;
};

// 编辑知识点
const editKnowledgePoint = (point) => {
  isEditing.value = true;
  currentKnowledgePoint.value = point;
  Object.assign(knowledgePointForm, {
    pointName: point.pointName,
    difficultyLevel: point.difficultyLevel,
    description: point.description,
    keywords: point.keywords
  });
  knowledgePointDialogVisible.value = true;
};

// 删除知识点
const deleteKnowledgePoint = async (point) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除知识点"${point.pointName}"吗？删除后无法恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );

    // 调用删除知识点API
    const response = await adminService.deleteKnowledgePoint(point.pointId);
    
    if (response.data.code === 0) {
      ElMessage.success('知识点删除成功');
      await loadKnowledgePoints();
      emit('refresh');
    } else {
      throw new Error(response.data.message || '删除知识点失败');
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除知识点失败:', error);
      ElMessage.error(error.message || '删除知识点失败，请稍后重试');
    }
  }
};

// 提交知识点表单
const submitKnowledgePointForm = async () => {
  try {
    await knowledgePointFormRef.value.validate();
    
    if (isEditing.value) {
      // 编辑模式：更新知识点信息
      const updateData = {
        pointId: currentKnowledgePoint.value.pointId,
        pointName: knowledgePointForm.pointName,
        description: knowledgePointForm.description,
        difficulty_level: knowledgePointForm.difficultyLevel,
        keywords: knowledgePointForm.keywords,
        courseId: props.courseId
      };
      
      // 调用更新知识点API
      const response = await adminService.updateKnowledgeName(updateData);
      
      if (response.data.code === 0) {
        ElMessage.success('知识点更新成功');
        knowledgePointDialogVisible.value = false;
        await loadKnowledgePoints();
        emit('refresh');
      } else {
        throw new Error(response.data.message || '更新知识点失败');
      }
    } else {
      // 添加模式：创建新知识点
      const createData = {
        pointName: knowledgePointForm.pointName,
        description: knowledgePointForm.description,
        difficulty_level: knowledgePointForm.difficultyLevel,
        keywords: knowledgePointForm.keywords,
        courseId: props.courseId
      };
      
      // 调用创建知识点API
      const response = await adminService.addKnowledgePoint(createData, props.chapter.chapterId);
      
      if (response.data.code === 0) {
        ElMessage.success('知识点创建成功');
        knowledgePointDialogVisible.value = false;
        await loadKnowledgePoints();
        emit('refresh');
      } else {
        throw new Error(response.data.message || '创建知识点失败');
      }
    }
  } catch (error) {
    console.error('提交知识点表单失败:', error);
    ElMessage.error(error.message || '操作失败，请稍后重试');
  }
};

// 重置知识点表单
const resetKnowledgePointForm = () => {
  Object.assign(knowledgePointForm, {
    pointName: '',
    difficultyLevel: 'easy',
    description: '',
    keywords: ''
  });
  currentKnowledgePoint.value = null;
  if (knowledgePointFormRef.value) {
    knowledgePointFormRef.value.resetFields();
  }
};

// 获取难度等级类型
const getDifficultyType = (level) => {
  const types = {
    easy: 'success',
    medium: 'warning',
    hard: 'danger'
  };
  return types[level] || 'info';
};

// 获取难度等级文本
const getDifficultyText = (level) => {
  const texts = {
    easy: '简单',
    medium: '中等',
    hard: '困难'
  };
  return texts[level] || '未知';
};

// 关闭对话框
const handleClose = () => {
  dialogVisible.value = false;
};
</script>

<style lang="less" scoped>
@text-primary: #1e293b;
@text-secondary: #64748b;
@border-color: rgba(230, 230, 230, 0.8);

.knowledge-point-management {
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;

    h4 {
      font-size: 16px;
      font-weight: 600;
      color: @text-primary;
      margin: 0;
    }
  }

  .knowledge-point-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .knowledge-point-item {
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

    .point-info {
      flex: 1;
      margin-right: 20px;

      .point-header {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 8px;

        .point-name {
          font-size: 15px;
          font-weight: 600;
          color: @text-primary;
          margin: 0;
        }
      }

      .point-description {
        color: @text-secondary;
        font-size: 14px;
        line-height: 1.6;
        margin: 0 0 12px 0;
      }

      .point-meta {
        .keywords {
          display: flex;
          align-items: center;
          gap: 6px;
          color: @text-secondary;
          font-size: 13px;
        }
      }
    }

    .point-actions {
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

.form-tip {
  font-size: 12px;
  color: @text-secondary;
  margin-top: 4px;
}

.dialog-footer {
  text-align: right;
}
</style>
