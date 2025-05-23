package com.aiproject.smartcampus.commons.utils;

import com.aiproject.smartcampus.exception.UserExpection;
import com.aiproject.smartcampus.pojo.dto.UserPreliminaryRegisterDTO;
import com.aiproject.smartcampus.pojo.dto.UserRegisterDTO;
import com.aiproject.smartcampus.pojo.po.ManagePerson;
import com.aiproject.smartcampus.pojo.po.Student;
import com.aiproject.smartcampus.pojo.po.Teacher;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;

/**
 * @program: SmartCampus
 * @description: 用户相关工具类
 * @author: lk
 * @create: 2025-05-20 12:38
 **/


public class Userutils {

    public static boolean preregisterInfoIsOK(UserPreliminaryRegisterDTO dto) {
        if (dto == null) {
            return false;
        }
        // 分别判断每个字段是否为 null 或 空白
        if (StringUtils.isBlank(dto.getCharacter())) {
            throw new UserExpection("用户特征为空");
        }
        if (StringUtils.isBlank(dto.getPassword())) {
            throw new UserExpection("用户密码为空");
        }
        if (StringUtils.isBlank(dto.getRePassword())) {
            throw  new UserExpection("第二次输入密码为空");
        }
        if (StringUtils.isBlank(dto.getPhone())) {
            throw  new UserExpection("手机号为空");
        }
        if (StringUtils.isBlank(dto.getUserAccount())) {
           throw  new UserExpection("用户账号为空");
        }
        if (!dto.getPassword().equals(dto.getRePassword())) {
            throw new UserExpection("两次输入密码不一致");
        }
        return true;
    }


    public static boolean validate(UserRegisterDTO dto) {
        if (dto == null || StringUtils.isBlank(dto.getCharacter())) {
            throw new UserExpection("注册角色标识（character）不能为空");  // 校验 DTO 与 character :contentReference[oaicite:0]{index=0}
        }

        switch (dto.getCharacter()) {
            case "Teacher":
                validateTeacher(dto.getTeacher());
                break;
            case "Student":
                validateStudent(dto.getStudent());
                break;
            case "ManagerPerson":
                validateAdmin(dto.getManagePerson());
                break;
            default:
                throw new UserExpection("未知角色类型：" + dto.getCharacter());
        }
        return true;
    }

    private static void validateTeacher(Teacher t) {
        if (t == null) {
            throw new UserExpection("教师信息未填写");  // 嵌套对象 null 校验
        }
        // 对 Teacher 中所有字段逐一校验
        if (StringUtils.isBlank(t.getAccount())) {
            throw new UserExpection("教师账号不能为空");  // StringUtils.isBlank 同时判 null/空字符串/全空白 :contentReference[oaicite:1]{index=1}
        }
        if (StringUtils.isBlank(t.getPassword())) {
            throw new UserExpection("教师密码不能为空");
        }
        if (StringUtils.isBlank(t.getTeacherName())) {
            throw new UserExpection("教师姓名不能为空");
        }
        if (t.getAge() == null || t.getAge() < 0) {
            throw new UserExpection("教师年龄不能为空且必须为非负整数");
        }
        if (StringUtils.isBlank(t.getPhone())) {
            throw new UserExpection("教师电话不能为空");
        }
        if (StringUtils.isBlank(t.getAddress())) {
            throw new UserExpection("教师地址不能为空");
        }
        if (StringUtils.isBlank(t.getDepartment())) {
            throw new UserExpection("教师所属院系不能为空");
        }
    }

    private static void validateStudent(Student s) {
        if (s == null) {
            throw new UserExpection("学生信息未填写");
        }
        // 对 Student 中所有字段逐一校验
        if (StringUtils.isBlank(s.getStudentAccount())) {
            throw new UserExpection("学生账号不能为空");
        }
        if (StringUtils.isBlank(s.getPassword())) {
            throw new UserExpection("学生密码不能为空");
        }
        if (StringUtils.isBlank(s.getStudentName())) {
            throw new UserExpection("学生姓名不能为空");
        }
        if (s.getAge() == null || s.getAge() < 0) {
            throw new UserExpection("学生年龄不能为空且必须为非负整数");
        }
        if (StringUtils.isBlank(s.getClassId())) {
            throw new UserExpection("班级号不能为空");
        }
        if (StringUtils.isBlank(s.getPhone())) {
            throw new UserExpection("学生电话不能为空");
        }
        if (StringUtils.isBlank(s.getAddress())) {
            throw new UserExpection("学生家庭住址不能为空");
        }
        if (s.getCounselorId() == null || s.getCounselorId() < 0) {
            throw new UserExpection("辅导员ID不能为空且必须为非负整数");
        }
        if (StringUtils.isBlank(s.getDepartment())) {
            throw new UserExpection("学生所属院系不能为空");
        }
    }

    private static void validateAdmin(ManagePerson m) {
        if (m == null) {
            throw new UserExpection("管理员信息未填写");
        }
        // 对 ManagePerson 中所有字段逐一校验
        if (StringUtils.isBlank(m.getAccount())) {
            throw new UserExpection("管理员账号不能为空");
        }
        if (StringUtils.isBlank(m.getPassword())) {
            throw new UserExpection("管理员密码不能为空");
        }
        if (StringUtils.isBlank(m.getName())) {
            throw new UserExpection("管理员姓名不能为空");
        }
        if (StringUtils.isBlank(m.getPhone())) {
            throw new UserExpection("管理员电话不能为空");
        }
    }


}









