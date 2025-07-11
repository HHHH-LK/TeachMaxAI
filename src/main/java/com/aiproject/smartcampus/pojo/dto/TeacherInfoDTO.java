package com.aiproject.smartcampus.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 教师基本信息DTO
 * 用于封装教师姓名、部门、邮箱、电话等基本信息
 * 
 * @author lk_hhh
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherInfoDTO implements Serializable {
    
    /**
     * 教师ID
     */
    private Integer teacherId;
    
    /**
     * 员工号
     */
    private String employeeNumber;
    
    /**
     * 所属部门
     */
    private String department;
    
    /**
     * 教师姓名
     */
    private String realName;
    
    /**
     * 邮箱地址
     */
    private String email;
    
    /**
     * 手机号码
     */
    private String phone;
    
    /**
     * 重写toString方法，使用中文描述
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("教师基本信息{");
        sb.append("教师姓名='").append(realName != null ? realName : "未填写").append('\'');
        sb.append(", 所属部门='").append(department != null ? department : "未分配").append('\'');
        sb.append(", 邮箱地址='").append(email != null ? email : "未填写").append('\'');
        sb.append(", 联系电话='").append(phone != null ? phone : "未填写").append('\'');
        sb.append(", 员工号='").append(employeeNumber != null ? employeeNumber : "未分配").append('\'');
        sb.append(", 教师编号=").append(teacherId);
        sb.append('}');
        return sb.toString();
    }
    
    /**
     * 获取简化的教师信息描述
     * @return 简化的描述信息
     */
    public String getSimpleInfo() {
        return String.format("教师：%s（%s）- %s", 
            realName != null ? realName : "未知", 
            department != null ? department : "未分配部门",
            email != null ? email : "无邮箱");
    }
    
    /**
     * 获取联系信息
     * @return 联系方式信息
     */
    public String getContactInfo() {
        StringBuilder contact = new StringBuilder();
        if (email != null && !email.trim().isEmpty()) {
            contact.append("邮箱：").append(email);
        }
        if (phone != null && !phone.trim().isEmpty()) {
            if (contact.length() > 0) {
                contact.append("，");
            }
            contact.append("电话：").append(phone);
        }
        return contact.length() > 0 ? contact.toString() : "暂无联系方式";
    }
    
    /**
     * 检查信息是否完整
     * @return true表示基本信息完整
     */
    public boolean isInfoComplete() {
        return realName != null && !realName.trim().isEmpty() &&
               department != null && !department.trim().isEmpty() &&
               (email != null && !email.trim().isEmpty() || 
                phone != null && !phone.trim().isEmpty());
    }
}