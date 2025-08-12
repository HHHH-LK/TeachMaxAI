import { markRaw } from 'vue';
import {
  Document,
  PieChart,
  Histogram
} from '@element-plus/icons-vue';

// 生成随机图标和样式
export const generateRandomIcon = () => {
  const icons = [
    {icon: markRaw(Document), style: 'background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%)'},
    {icon: markRaw(PieChart), style: 'background: linear-gradient(135deg, #ec4899 0%, #f472b6 100%)'},
    {icon: markRaw(Histogram), style: 'background: linear-gradient(135deg, #06b6d4 0%, #22d3ee 100%)'}
  ];
  return icons[Math.floor(Math.random() * icons.length)];
};

// 生成课程描述
export const generateCourseDescription = (courseName) => {
  const descriptions = [
    `深入学习${courseName}核心知识，掌握实战技能。`,
    `系统学习${courseName}基础理论和应用方法。`,
    `通过理论与实践相结合的方式学习${courseName}。`,
    `掌握${courseName}的关键概念和实用技巧。`,
    `学习${courseName}的最新发展趋势和前沿技术。`
  ];
  return descriptions[Math.floor(Math.random() * descriptions.length)];
};

// 生成头像颜色
export const generateAvatarColor = (id) => {
  const colors = [
    '#6366f1', '#ec4899', '#06b6d4', '#8b5cf6', '#f472b6', 
    '#22d3ee', '#10b981', '#f59e0b', '#ef4444', '#84cc16'
  ];
  return colors[id % colors.length];
};

// 智能生成课程信息
export const generateCourse = (title, time, teachers = []) => {
  const iconData = generateRandomIcon();
  const description = generateCourseDescription(title);

  const today = new Date();
  const randomDays = Math.floor(Math.random() * 7) + 1;
  today.setDate(today.getDate() + randomDays);

  const formattedDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')} ${String(Math.floor(Math.random() * 6) + 14).padStart(2, '0')}:${Math.random() > 0.5 ? '30' : '00'}`;

  return {
    id: Date.now(), // 生成唯一ID
    title,
    teacherList: teachers,
    time,
    description: description,
    date: formattedDate,
    icon: iconData.icon,
    iconStyle: iconData.style,
    status: 'active'
  };
};

// 处理课程数据，转换为前端需要的格式
export const processCourseData = (courseList, teachers = []) => {
  return courseList.map(course => {
    // 为每个课程生成随机图标和样式
    const iconData = generateRandomIcon();
    
    // 处理教师信息
    let teacherList = [];
    if (course.teacher && course.teacher.user) {
      teacherList = [{
        id: course.teacher.teacherId,
        name: course.teacher.user.realName || '未知教师',
        subject: course.teacher.department || '未指定专业',
        avatarColor: generateAvatarColor(course.teacher.teacherId)
      }];
    }
    
    // 生成课程描述
    const description = generateCourseDescription(course.courseName);
    
    return {
      id: course.courseId,
      title: course.courseName,
      teacherList: teacherList,
      time: course.semester || '2024春季',
      description: description,
      date: course.createdAt ? course.createdAt.split(' ')[0] : '2025-07-16',
      icon: iconData.icon,
      iconStyle: iconData.style,
      status: course.status || 'active'
    };
  });
};

// 处理教师数据，转换为前端需要的格式
export const processTeacherData = (userList) => {
  return userList
    .filter(item => item.teacher && item.user) // 确保有teacher和user对象
    .map(item => ({
      id: item.teacher.teacherId,
      name: item.user.realName || '未知教师',
      subject: item.teacher.department || '未指定专业',
      avatarColor: generateAvatarColor(item.teacher.teacherId)
    }));
};
