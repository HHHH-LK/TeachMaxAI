# 考核管理组件拆分说明

## 概述
将原来的大型 `AssessmentManagement.vue` 组件拆分为多个低耦合的子组件，提高代码的可维护性和复用性。

## 组件结构

### 主组件
- **AssessmentManagement.vue** - 主组件，负责整体布局和状态管理

### 子组件
1. **AssessmentList.vue** - 考核列表展示组件
   - 包含全部考核和待阅卷两个标签页
   - 负责渲染考核卡片列表

2. **AssessmentCard.vue** - 单个考核卡片组件
   - 显示考核的基本信息（标题、类型、状态、统计数据）
   - 提供操作按钮（查看、考试内容、智能阅卷、删除）

3. **PendingAssessmentCard.vue** - 待阅卷考核卡片组件
   - 专门用于显示待阅卷的考核
   - 显示待阅卷数量和提交时间
   - 提供智能阅卷和查看提交按钮

4. **GenerateExamModal.vue** - 智能生成试卷模态框组件
   - 包含试卷生成表单
   - 处理试卷生成逻辑
   - 显示生成的试卷内容和报告

5. **GradingModal.vue** - 智能阅卷模态框组件
   - 显示学生提交列表
   - 提供一键智能阅卷功能
   - 集成学生答题详情查看

6. **SubmissionDetailModal.vue** - 学生答题详情模态框组件
   - 显示学生的详细答题情况
   - 支持手动评分和AI评分
   - 提供单题智能评分功能

7. **AssessmentDetailModal.vue** - 考核详情模态框组件
   - 显示考核的详细信息
   - 包含章节和知识点标签

8. **ExamContentModal.vue** - 考试内容模态框组件
   - 显示试卷内容
   - 使用 ExamAttempt 组件以只读模式展示

## 组件通信

### Props 传递
- 主组件向子组件传递数据（如考核列表、选中的考核等）
- 子组件通过 props 接收数据

### 事件通信
- 子组件通过 `$emit` 向父组件发送事件
- 主组件监听子组件事件并处理相应的业务逻辑

### 主要事件
- `view` - 查看考核详情
- `view-exam` - 查看考试内容
- `grade` - 开始智能阅卷
- `delete` - 删除考核
- `view-submissions` - 查看学生提交
- `generated` - 试卷生成成功
- `save-submission` - 保存学生评分

## 优势

1. **低耦合** - 每个组件职责单一，相互依赖较少
2. **易维护** - 代码分散到多个文件中，便于定位和修改
3. **可复用** - 子组件可以在其他地方复用
4. **易测试** - 每个组件可以独立测试
5. **性能优化** - 可以针对性地优化特定组件

## 使用方式

```vue
<template>
  <div class="assessment-management">
    <!-- 考核列表 -->
    <AssessmentList
      :assessments="assessments"
      @view="viewAssessment"
      @view-exam="viewExamContent"
      @grade="startGrading"
      @delete="deleteAssessment"
    />

    <!-- 各种模态框 -->
    <GenerateExamModal v-model="showGenerateModal" @generated="handleExamGenerated" />
    <GradingModal v-model="showGradingModal" :assessment="selectedAssessment" />
    <AssessmentDetailModal v-model="showViewModal" :assessment="selectedAssessment" />
    <ExamContentModal v-model="showExamModal" :examPaper="selectedExamPaper" />
  </div>
</template>
```

## 注意事项

1. 确保所有子组件都正确导入
2. 注意事件名称的一致性
3. 保持 props 的类型定义准确
4. 遵循 Vue 3 Composition API 的最佳实践 