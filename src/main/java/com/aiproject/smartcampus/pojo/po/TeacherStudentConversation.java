package com.aiproject.smartcampus.pojo.po;

public class TeacherStudentConversation {
    private Long id;           // 主键（自增）
    private Long studentId;    // 学生ID
    private Long teacherId;    // 教师ID

    // 必须提供 getter 和 setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }
}