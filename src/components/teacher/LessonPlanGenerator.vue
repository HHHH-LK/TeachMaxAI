<template>
  <div class="lesson-plan-generator-container">


    <el-card class="generator-card">
      <template #header>
        <div class="card-header">
          <span>智能教案生成器</span>
          <el-tag type="success" size="small">AI驱动</el-tag>
        </div>
      </template>

      <el-form :model="form" :rules="rules" ref="lessonPlanForm" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="课程名称" prop="courseName">
              <el-input v-model="form.courseName" placeholder="如：数据结构、算法设计等" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="授课班级" prop="className">
              <el-input v-model="form.className" placeholder="如：2023级计算机1班" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="授课时长" prop="duration">
              <el-select v-model="form.duration" placeholder="选择授课时长">
                <el-option label="45分钟" value="45分钟" />
                <el-option label="90分钟" value="90分钟" />
                <el-option label="120分钟" value="120分钟" />
                <el-option label="180分钟" value="180分钟" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程类型" prop="courseType">
              <el-select v-model="form.courseType" placeholder="选择课程类型">
                <el-option label="理论课" value="theory" />
                <el-option label="实验课" value="lab" />
                <el-option label="研讨课" value="seminar" />
                <el-option label="混合课" value="mixed" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="学生水平" prop="studentLevel">
              <el-select v-model="form.studentLevel" placeholder="选择学生水平">
                <el-option label="初级" value="beginner" />
                <el-option label="中级" value="intermediate" />
                <el-option label="高级" value="advanced" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="教学重点" prop="focus">
              <el-select v-model="form.focus" placeholder="选择教学重点">
                <el-option label="概念理解" value="concept" />
                <el-option label="技能培养" value="skill" />
                <el-option label="应用实践" value="application" />
                <el-option label="综合能力" value="comprehensive" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="教学内容" prop="content">
          <el-input
              v-model="form.content"
              type="textarea"
              :rows="4"
              placeholder="请详细描述本节课的教学内容，例如：本节课主要讲解数据结构中链表的概念、基本操作（插入、删除、查找）以及实际应用场景。重点培养学生的编程思维和算法设计能力。"
          />
        </el-form-item>

        <el-form-item label="特殊要求" prop="requirements">
          <el-input
              v-model="form.requirements"
              type="textarea"
              :rows="3"
              placeholder="如有特殊教学要求或注意事项，请在此说明（可选）"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="generateLessonPlan" :loading="generating">
            <el-icon><MagicStick /></el-icon>
            智能生成教案
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 等待动画区域 -->
      <div v-if="generating" class="loading-area">
        <div class="loading-container">
          <div class="ai-brain">
            <div class="brain-core">
              <div class="core-inner"></div>
            </div>
            <div class="brain-pulse"></div>
            <div class="brain-pulse delay-1"></div>
            <div class="brain-pulse delay-2"></div>
            <div class="brain-pulse delay-3"></div>
          </div>
          <div class="loading-text">
            <h3>AI正在为您生成教案</h3>
            <p class="loading-message">{{ loadingTexts[currentLoadingText] }}</p>
            <div class="loading-dots">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: loadingProgress + '%' }"></div>
            <div class="progress-glow"></div>
          </div>
          <div class="loading-steps">
            <div class="step" :class="{ active: loadingProgress > 20 }">
              <div class="step-icon">📚</div>
              <span>分析课程内容</span>
            </div>
            <div class="step" :class="{ active: loadingProgress > 40 }">
              <div class="step-icon">🎯</div>
              <span>设计教学目标</span>
            </div>
            <div class="step" :class="{ active: loadingProgress > 60 }">
              <div class="step-icon">📝</div>
              <span>生成教案结构</span>
            </div>
            <div class="step" :class="{ active: loadingProgress > 80 }">
              <div class="step-icon">✨</div>
              <span>优化完善内容</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 生成的教案内容 -->
      <div v-else class="markdown-content" v-html="renderedMarkdown"></div>

      <div class="plan-actions" style="margin-top: 20px;">
        <el-button type="primary" @click="exportPlan">
          <el-icon><Download /></el-icon>
          导出PDF
        </el-button>
        <el-button type="success" @click="savePlan">
          <el-icon><Check /></el-icon>
          保存教案
        </el-button>
        <el-button @click="regeneratePlan">
          <el-icon><Refresh /></el-icon>
          重新生成
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue';
import { ElMessage } from 'element-plus';
import {
  MagicStick, Download, Check, Refresh
} from '@element-plus/icons-vue';

import { marked } from 'marked';
import DOMPurify from 'dompurify';
import { teacherService } from '@/services/api';
import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';

const lessonPlanForm = ref(null);
const generating = ref(false);
const loadingProgress = ref(0);
const loadingTexts = ref([
  '正在分析课程内容...',
  '正在设计教学目标...',
  '正在生成教案结构...',
  '正在优化教学内容...',
  '正在完善教学流程...',
  '正在生成最终教案...'
]);
const currentLoadingText = ref(0);

const form = reactive({
  courseName: '',
  className: '',
  duration: '90分钟',
  courseType: 'theory',
  studentLevel: 'intermediate',
  focus: 'concept',
  content: '',
  requirements: ''
});

const rules = {
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  className: [{ required: true, message: '请输入授课班级', trigger: 'blur' }],
  duration: [{ required: true, message: '请选择授课时长', trigger: 'change' }],
  courseType: [{ required: true, message: '请选择课程类型', trigger: 'change' }],
  studentLevel: [{ required: true, message: '请选择学生水平', trigger: 'change' }],
  focus: [{ required: true, message: '请选择教学重点', trigger: 'change' }],
  content: [{ required: true, message: '请输入教学内容', trigger: 'blur' }]
};

const generatedPlan = ref({});

// 从不同可能的字段提取 Markdown 字符串
const extractMarkdownText = (raw) => {
  if (!raw) return '';
  if (typeof raw === 'string') return raw;
  if (raw.data && typeof raw.data === 'string') return raw.data;
  if (raw.content && typeof raw.content === 'string') return raw.content;
  if (raw.result && typeof raw.result === 'string') return raw.result;
  return JSON.stringify(raw, null, 2);
};

// 预处理 Markdown 内容，避免特殊符号引起解析问题
const preprocessMarkdown = (markdown) => {
  return markdown
      .replace(/^(\s*)\d+\.\s/gm, '$1- ')
      .replace(/(```[\s\S]*?```)/g, match => match.replace(/\|/g, '│'))
      .replace(/```([^`]+?)$/gs, '```$1\n```');
};

//md解析器
const renderedMarkdown = computed(() => {
  try {
    if (!generatedPlan.value || Object.keys(generatedPlan.value).length === 0) {
      return '';
    }

    const rawContent = extractMarkdownText(generatedPlan.value);
    if (!rawContent) return '';

    const preprocessed = preprocessMarkdown(rawContent);

    const rawHtml = marked.parse(preprocessed, {
      gfm: true,
      breaks: true,
      headerIds: false,
      mangle: false
    });

    const cleanHtml = DOMPurify.sanitize(rawHtml, {
      ALLOWED_TAGS: ['div', 'h1', 'h2', 'h3', 'h4', 'p', 'ul', 'ol', 'li',
        'table', 'tr', 'td', 'th', 'pre', 'code', 'span', 'strong', 'em'],
      ADD_ATTR: ['class']
    });

    return `<div class="markdown-styles">${cleanHtml}</div>`;
  } catch (e) {
    console.error('Markdown 渲染失败:', e);
    return `<p style="color:red;">教案渲染失败，请稍后重试</p>`;
  }
});

const buildLessonPlanData = (form) => {
  const courseTypeMap = {
    'theory': '理论课',
    'lab': '实验课',
    'seminar': '研讨课',
    'mixed': '混合课'
  };
  const studentLevelMap = {
    'beginner': '初级',
    'intermediate': '中级',
    'advanced': '高级'
  };
  const focusMap = {
    'concept': '概念理解',
    'skill': '技能培养',
    'application': '应用实践',
    'comprehensive': '综合能力'
  };

  const courseTypeText = courseTypeMap[form.courseType] || form.courseType;
  const studentLevelText = studentLevelMap[form.studentLevel] || form.studentLevel;
  const focusText = focusMap[form.focus] || form.focus;

  return `课程名称：${form.courseName}。授课班级：${form.className}。授课时长：${form.duration}。课程类型：${courseTypeText}。学生水平：${studentLevelText}。教学重点：${focusText}。教学内容：${form.content}。特殊要求：${form.requirements || '无特殊要求'}。`;
};

const generateMockLessonPlan = () => {
  generatedPlan.value = `
# 模拟教案示例

课程名称：${form.courseName}
授课班级：${form.className}
授课时长：${form.duration}
课程类型：理论课
学生水平：中级
教学重点：概念理解

## 教学内容
本节课主要讲解数据结构中链表的概念、基本操作（插入、删除、查找）以及实际应用场景。

## 特殊要求
无特殊要求。
`;
};

const generateLessonPlan = async () => {
  if (!lessonPlanForm.value) return;

  lessonPlanForm.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('请先完善表单信息');
      return;
    }

    generating.value = true;
    loadingProgress.value = 0;
    currentLoadingText.value = 0;
    
    // 启动进度条动画
    const progressInterval = setInterval(() => {
      if (loadingProgress.value < 90) {
        loadingProgress.value += Math.random() * 15;
      }
    }, 500);
    
    // 启动文本轮换动画
    const textInterval = setInterval(() => {
      currentLoadingText.value = (currentLoadingText.value + 1) % loadingTexts.value.length;
    }, 2000);

    try {
      const lessonPlanData = buildLessonPlanData(form);
      generatedPlan.value = '';
      ElMessage.info('正在根据备课资料生成教案...');

      const response = await teacherService.generateLessonPlan(lessonPlanData, 1, 1);

      if (response.data?.success) {
        // 这里做兼容处理，防止返回结构不一致
        const responseContent = response.data.data;
        generatedPlan.value = typeof responseContent === 'string'
            ? responseContent
            : (responseContent?.content || responseContent?.result || JSON.stringify(responseContent));
      } else {
        ElMessage.warning('后端API不可用，使用模拟数据');
        generateMockLessonPlan();
      }
    } catch (error) {
      console.error('教案生成失败:', error);
      ElMessage.warning('后端API不可用，使用模拟数据');
      generateMockLessonPlan();
    } finally {
      loadingProgress.value = 100;
      setTimeout(() => {
        generating.value = false;
        loadingProgress.value = 0;
      }, 500);
      clearInterval(progressInterval);
      clearInterval(textInterval);
    }
  });
};

const resetForm = () => {
  lessonPlanForm.value?.resetFields();
  generatedPlan.value = {};
};

const exportPlan = async () => {
  if (!generatedPlan.value || Object.keys(generatedPlan.value).length === 0) {
    ElMessage.warning('请先生成教案');
    return;
  }

  try {
    ElMessage.info('正在生成PDF文件...');
    
    // 创建临时容器用于PDF生成
    const tempContainer = document.createElement('div');
    tempContainer.style.cssText = `
      position: fixed;
      left: -9999px;
      top: -9999px;
      width: 800px;
      background: white;
      padding: 40px;
      font-family: 'Microsoft YaHei', Arial, sans-serif;
      line-height: 1.6;
      color: #333;
    `;
    
    // 生成PDF内容
    const pdfContent = generatePDFContent();
    tempContainer.innerHTML = pdfContent;
    document.body.appendChild(tempContainer);

    // 使用html2canvas将内容转换为图片
    const canvas = await html2canvas(tempContainer, {
      scale: 2,
      useCORS: true,
      allowTaint: true,
      backgroundColor: '#ffffff',
      width: 800,
      height: tempContainer.scrollHeight
    });

    // 移除临时容器
    document.body.removeChild(tempContainer);

    // 计算PDF页面
    const imgWidth = 210; // A4纸宽度(mm)
    const pageHeight = 297; // A4纸高度(mm)
    const imgHeight = (canvas.height * imgWidth) / canvas.width;
    let heightLeft = imgHeight;

    // 创建PDF文档
    const pdf = new jsPDF('p', 'mm', 'a4');
    let position = 0;

    // 添加第一页
    pdf.addImage(canvas, 'PNG', 0, position, imgWidth, imgHeight);
    heightLeft -= pageHeight;

    // 如果内容超过一页，添加更多页面
    while (heightLeft >= 0) {
      position = heightLeft - imgHeight;
      pdf.addPage();
      pdf.addImage(canvas, 'PNG', 0, position, imgWidth, imgHeight);
      heightLeft -= pageHeight;
    }

    // 下载PDF文件
    const fileName = `${form.courseName}_教案_${new Date().toISOString().slice(0, 10)}.pdf`;
    pdf.save(fileName);

    ElMessage.success('PDF教案导出成功！');
  } catch (error) {
    console.error('PDF导出失败:', error);
    ElMessage.error('PDF导出失败，请重试');
  }
};

const generatePDFContent = () => {
  const courseTypeMap = {
    'theory': '理论课',
    'lab': '实验课',
    'seminar': '研讨课',
    'mixed': '混合课'
  };
  const studentLevelMap = {
    'beginner': '初级',
    'intermediate': '中级',
    'advanced': '高级'
  };
  const focusMap = {
    'concept': '概念理解',
    'skill': '技能培养',
    'application': '应用实践',
    'comprehensive': '综合能力'
  };

  const courseTypeText = courseTypeMap[form.courseType] || form.courseType;
  const studentLevelText = studentLevelMap[form.studentLevel] || form.studentLevel;
  const focusText = focusMap[form.focus] || form.focus;

  let content = `
    <div style="text-align: center; margin-bottom: 30px;">
      <h1 style="color: #3b82f6; font-size: 28px; margin-bottom: 10px;">智能教案</h1>
      <p style="color: #666; font-size: 14px;">AI智能生成 · ${new Date().toLocaleString('zh-CN')}</p>
    </div>
    
    <div style="background: #f8fafc; padding: 20px; border-radius: 8px; margin-bottom: 25px;">
      <h2 style="color: #3b82f6; font-size: 20px; margin-bottom: 15px;">基本信息</h2>
      <table style="width: 100%; border-collapse: collapse;">
        <tr>
          <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold; width: 120px;">课程名称</td>
          <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">${form.courseName}</td>
        </tr>
        <tr>
          <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold;">授课班级</td>
          <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">${form.className}</td>
        </tr>
        <tr>
          <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold;">授课时长</td>
          <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">${form.duration}</td>
        </tr>
        <tr>
          <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold;">课程类型</td>
          <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">${courseTypeText}</td>
        </tr>
        <tr>
          <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold;">学生水平</td>
          <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">${studentLevelText}</td>
        </tr>
        <tr>
          <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold;">教学重点</td>
          <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">${focusText}</td>
        </tr>
      </table>
    </div>
  `;

  // 添加教学内容
  if (form.content) {
    content += `
      <div style="margin-bottom: 25px;">
        <h2 style="color: #3b82f6; font-size: 20px; margin-bottom: 15px;">教学内容</h2>
        <div style="background: #f8fafc; padding: 15px; border-radius: 6px; border-left: 4px solid #3b82f6;">
          <p style="margin: 0; line-height: 1.6;">${form.content}</p>
        </div>
      </div>
    `;
  }

  // 添加特殊要求
  if (form.requirements) {
    content += `
      <div style="margin-bottom: 25px;">
        <h2 style="color: #3b82f6; font-size: 20px; margin-bottom: 15px;">特殊要求</h2>
        <div style="background: #fef3c7; padding: 15px; border-radius: 6px; border-left: 4px solid #f59e0b;">
          <p style="margin: 0; line-height: 1.6;">${form.requirements}</p>
        </div>
      </div>
    `;
  }

  // 添加生成的教案内容
  if (typeof generatedPlan.value === 'string') {
    // 处理Markdown内容
    const processedContent = generatedPlan.value
      .replace(/^#\s+(.+)$/gm, '<h2 style="color: #3b82f6; font-size: 18px; margin: 20px 0 10px 0;">$1</h2>')
      .replace(/^##\s+(.+)$/gm, '<h3 style="color: #1f2937; font-size: 16px; margin: 15px 0 8px 0;">$1</h3>')
      .replace(/^###\s+(.+)$/gm, '<h4 style="color: #374151; font-size: 14px; margin: 12px 0 6px 0;">$1</h4>')
      .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.+?)\*/g, '<em>$1</em>')
      .replace(/`(.+?)`/g, '<code style="background: #f3f4f6; padding: 2px 4px; border-radius: 3px; font-family: monospace;">$1</code>')
      .replace(/^\s*[-*+]\s+(.+)$/gm, '<li style="margin: 5px 0;">$1</li>')
      .replace(/^\s*\d+\.\s+(.+)$/gm, '<li style="margin: 5px 0;">$1</li>')
      .replace(/\n\n/g, '</p><p>')
      .replace(/^(.+)$/gm, '<p style="margin: 8px 0; line-height: 1.6;">$1</p>');

    content += `
      <div style="margin-bottom: 25px;">
        <h2 style="color: #3b82f6; font-size: 20px; margin-bottom: 15px;">教案详情</h2>
        <div style="background: white; padding: 20px; border-radius: 8px; border: 1px solid #e5e7eb;">
          ${processedContent}
        </div>
      </div>
    `;
  } else {
    // 处理对象格式的内容
    if (generatedPlan.value.objectives) {
      content += `
        <div style="margin-bottom: 20px;">
          <h3 style="color: #1f2937; font-size: 16px; margin-bottom: 10px;">教学目标</h3>
          <ul style="margin: 0; padding-left: 20px;">
            ${generatedPlan.value.objectives.map(obj => `<li style="margin: 5px 0;">${obj}</li>`).join('')}
          </ul>
        </div>
      `;
    }

    if (generatedPlan.value.methodology) {
      content += `
        <div style="margin-bottom: 20px;">
          <h3 style="color: #1f2937; font-size: 16px; margin-bottom: 10px;">教学方法</h3>
          <ul style="margin: 0; padding-left: 20px;">
            ${generatedPlan.value.methodology.map(method => `<li style="margin: 5px 0;">${method}</li>`).join('')}
          </ul>
        </div>
      `;
    }

    if (generatedPlan.value.keyPoints) {
      content += `
        <div style="margin-bottom: 20px;">
          <h3 style="color: #1f2937; font-size: 16px; margin-bottom: 10px;">教学重点</h3>
          <p style="margin: 0; padding: 10px; background: #fef3c7; border-radius: 4px;">${generatedPlan.value.keyPoints}</p>
        </div>
      `;
    }

    if (generatedPlan.value.difficulties) {
      content += `
        <div style="margin-bottom: 20px;">
          <h3 style="color: #1f2937; font-size: 16px; margin-bottom: 10px;">教学难点</h3>
          <p style="margin: 0; padding: 10px; background: #fee2e2; border-radius: 4px;">${generatedPlan.value.difficulties}</p>
        </div>
      `;
    }

    if (generatedPlan.value.content) {
      content += `
        <div style="margin-bottom: 20px;">
          <h3 style="color: #1f2937; font-size: 16px; margin-bottom: 10px;">教学内容</h3>
          <div style="padding: 15px; background: #f8fafc; border-radius: 6px;">
            ${Array.isArray(generatedPlan.value.content) 
              ? generatedPlan.value.content.map(section => `
                  <div style="margin-bottom: 15px;">
                    <h4 style="color: #374151; margin-bottom: 8px;">${section.sectionTitle}</h4>
                    <p style="margin: 5px 0;">${section.details}</p>
                    ${section.keyPoints ? `<p style="margin: 5px 0; color: #059669;"><strong>重点：</strong>${section.keyPoints}</p>` : ''}
                  </div>
                `).join('')
              : `<p style="margin: 0;">${generatedPlan.value.content}</p>`
            }
          </div>
        </div>
      `;
    }

    if (generatedPlan.value.process) {
      content += `
        <div style="margin-bottom: 20px;">
          <h3 style="color: #1f2937; font-size: 16px; margin-bottom: 10px;">教学过程</h3>
          <div style="padding: 15px; background: #f8fafc; border-radius: 6px;">
            ${generatedPlan.value.process.map((step, index) => `
              <div style="margin-bottom: 15px;">
                <h4 style="color: #374151; margin-bottom: 8px;">${index + 1}. ${step.stepTitle}</h4>
                <p style="margin: 0;">${step.description}</p>
              </div>
            `).join('')}
          </div>
        </div>
      `;
    }

    if (generatedPlan.value.blackboardDesign) {
      content += `
        <div style="margin-bottom: 20px;">
          <h3 style="color: #1f2937; font-size: 16px; margin-bottom: 10px;">板书设计</h3>
          <p style="margin: 0; padding: 10px; background: #f0f9ff; border-radius: 4px;">${generatedPlan.value.blackboardDesign}</p>
        </div>
      `;
    }

    if (generatedPlan.value.homework) {
      content += `
        <div style="margin-bottom: 20px;">
          <h3 style="color: #1f2937; font-size: 16px; margin-bottom: 10px;">课后作业</h3>
          <p style="margin: 0; padding: 10px; background: #f0fdf4; border-radius: 4px;">${generatedPlan.value.homework}</p>
        </div>
      `;
    }

    if (generatedPlan.value.reflection) {
      content += `
        <div style="margin-bottom: 20px;">
          <h3 style="color: #1f2937; font-size: 16px; margin-bottom: 10px;">教学反思</h3>
          <p style="margin: 0; padding: 10px; background: #fef7ff; border-radius: 4px;">${generatedPlan.value.reflection}</p>
        </div>
      `;
    }
  }

  // 添加页脚
  content += `
    <div style="margin-top: 40px; padding-top: 20px; border-top: 1px solid #e5e7eb; text-align: center; color: #6b7280; font-size: 12px;">
      <p>本教案由AI智能生成系统创建</p>
      <p>生成时间：${new Date().toLocaleString('zh-CN')}</p>
    </div>
  `;

  return content;
};

const savePlan = () => {
  ElMessage.success('教案保存成功！');
};

const regeneratePlan = () => {
  generateLessonPlan();
};
</script>


<style scoped>
.markdown-styles {
  /* 添加基础样式确保内容可读 */
  line-height: 1.6;
  color: #333;
}

/* 表格样式修复 */
.markdown-styles table {
  width: 100%;
  border-collapse: collapse;
  margin: 1em 0;
}

.markdown-styles th, .markdown-styles td {
  border: 1px solid #ddd;
  padding: 8px;
  text-align: left;
}

/* 代码块样式 */
.markdown-styles pre {
  background-color: #f5f5f5;
  padding: 1em;
  overflow: auto;
  border-radius: 4px;
}

/* 错误信息样式 */
.markdown-error {
  padding: 15px;
  background-color: #fff8f8;
  border: 1px solid #ffd6d6;
  border-radius: 4px;
  color: #c00;
}

.markdown-error pre {
  background-color: #f0f0f0;
  padding: 10px;
  overflow: auto;
  max-height: 200px;
}

.lesson-plan-generator-container {
  padding: 20px;
}

.generator-card {
  min-height: 800px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.generated-plan-display {
  margin-top: 20px;
}

.plan-header {
  text-align: center;
  margin-bottom: 30px;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: white;
}

.plan-header h2 {
  margin: 0 0 15px 0;
  font-size: 28px;
  font-weight: 600;
}

.plan-meta {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}

.plan-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section {
  margin-bottom: 20px;
}

.section h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #2c3e50;
  font-size: 18px;
  margin-bottom: 15px;
  padding-bottom: 8px;
  border-bottom: 2px solid #e1e8ed;
}

.section-card {
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.goal-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
  padding: 8px;
  background: #f8f9fa;
  border-radius: 6px;
}

.goal-number {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: #3b82f6;
  color: white;
  border-radius: 50%;
  font-size: 12px;
  font-weight: bold;
  flex-shrink: 0;
}

.goal-text {
  line-height: 1.6;
  color: #374151;
}

.content-details {
  padding: 8px 0;
}

.content-details p {
  margin: 0 0 8px 0;
  line-height: 1.6;
  color: #4b5563;
}

.key-points {
  background: #fef3c7;
  padding: 8px 12px;
  border-radius: 6px;
  border-left: 4px solid #f59e0b;
  font-size: 14px;
}

.methodology-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.method-tag {
  margin-bottom: 4px;
}

.key-points-section,
.difficult-points-section {
  margin-bottom: 16px;
}

.key-points-section h4,
.difficult-points-section h4 {
  color: #2c3e50;
  margin: 0 0 8px 0;
  font-size: 16px;
}

.key-points-section p,
.difficult-points-section p {
  margin: 0;
  line-height: 1.6;
  color: #4b5563;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 6px;
}

.blackboard-content,
.homework-content,
.reflection-content {
  padding: 8px 0;
}

.blackboard-content p,
.homework-content p,
.reflection-content p {
  margin: 0;
  line-height: 1.6;
  color: #4b5563;
}

.plan-actions {
  display: flex;
  justify-content: center;
  gap: 15px;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #e1e8ed;
}

.plan-sidebar {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.plan-sidebar .section {
  margin-bottom: 0;
}

.plan-sidebar .section-card {
  margin-bottom: 0;
}

/* 等待动画区域样式 */
.loading-area {
  margin: 30px 0;
  padding: 40px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.08) 0%, rgba(37, 99, 235, 0.08) 100%);
  border-radius: 15px;
  border: 1px solid rgba(59, 130, 246, 0.15);
  position: relative;
  overflow: hidden;
}

.loading-area::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  animation: shimmer 2s infinite;
}

@keyframes shimmer {
  0% {
    left: -100%;
  }
  100% {
    left: 100%;
  }
}

.loading-container {
  text-align: center;
  max-width: 100%;
  position: relative;
}

.ai-brain {
  position: relative;
  width: 80px;
  height: 80px;
  margin: 0 auto 25px;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  animation: brain-rotate 3s infinite linear;
  box-shadow: 0 8px 25px rgba(59, 130, 246, 0.4);
}

@keyframes brain-rotate {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.brain-core {
  position: absolute;
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, #fff, #f8fafc);
  border-radius: 50%;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  box-shadow: inset 0 2px 10px rgba(0, 0, 0, 0.1);
}

.core-inner {
  position: absolute;
  width: 30px;
  height: 30px;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border-radius: 50%;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation: core-pulse 2s infinite ease-in-out;
}

@keyframes core-pulse {
  0%, 100% {
    transform: translate(-50%, -50%) scale(0.8);
    opacity: 0.8;
  }
  50% {
    transform: translate(-50%, -50%) scale(1.2);
    opacity: 1;
  }
}

.brain-pulse {
  position: absolute;
  width: 100%;
  height: 100%;
  border: 3px solid rgba(255, 255, 255, 0.6);
  border-radius: 50%;
  animation: pulse 2s infinite;
}

.brain-pulse.delay-1 {
  animation-delay: -0.5s;
}

.brain-pulse.delay-2 {
  animation-delay: -1s;
}

.brain-pulse.delay-3 {
  animation-delay: -1.5s;
}

@keyframes pulse {
  0% {
    transform: scale(0.8);
    opacity: 0.8;
  }
  50% {
    transform: scale(1.3);
    opacity: 0.3;
  }
  100% {
    transform: scale(0.8);
    opacity: 0.8;
  }
}

.loading-text h3 {
  color: #1f2937;
  margin-bottom: 15px;
  font-size: 22px;
  font-weight: 600;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.loading-text p {
  color: #6b7280;
  margin-bottom: 25px;
  font-size: 16px;
  line-height: 1.6;
}

.loading-message {
  min-height: 24px;
  animation: text-fade 0.5s ease-in-out;
}

@keyframes text-fade {
  0% {
    opacity: 0;
    transform: translateY(10px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

.loading-dots {
  margin-bottom: 20px;
}

.loading-dots span {
  display: inline-block;
  width: 12px;
  height: 12px;
  margin: 0 6px;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border-radius: 50%;
  animation: dot-pulse 1.4s infinite ease-in-out;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.4);
}

.loading-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.loading-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

.loading-dots span:nth-child(3) {
  animation-delay: 0s;
}

@keyframes dot-pulse {
  0%, 80%, 100% {
    transform: translateY(0) scale(1);
  }
  40% {
    transform: translateY(-12px) scale(1.2);
  }
}

.progress-bar {
  width: 100%;
  height: 12px;
  background: rgba(229, 231, 235, 0.5);
  border-radius: 6px;
  overflow: hidden;
  margin: 25px 0;
  position: relative;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1);
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #2563eb, #1d4ed8);
  border-radius: 6px;
  transition: width 0.5s ease-in-out;
  position: relative;
  box-shadow: 0 0 20px rgba(59, 130, 246, 0.6);
}

.progress-glow {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  width: 30px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.6), transparent);
  animation: progress-glow 2s infinite;
}

@keyframes progress-glow {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(400%);
  }
}

/* 加载步骤样式 */
.loading-steps {
  display: flex;
  justify-content: space-between;
  margin-top: 25px;
  padding: 0 5px;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  opacity: 0.3;
  transition: all 0.5s ease;
  transform: scale(0.8);
}

.step.active {
  opacity: 1;
  transform: scale(1);
  color: #3b82f6;
}

.step-icon {
  font-size: 20px;
  margin-bottom: 6px;
  animation: step-bounce 0.6s ease-in-out;
}

.step.active .step-icon {
  animation: step-bounce 0.6s ease-in-out;
}

@keyframes step-bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

.step span {
  font-size: 11px;
  color: #6b7280;
  text-align: center;
  line-height: 1.3;
  transition: color 0.3s ease;
}

.step.active span {
  color: #3b82f6;
  font-weight: 500;
}
</style>