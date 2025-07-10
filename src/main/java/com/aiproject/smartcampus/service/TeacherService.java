package com.aiproject.smartcampus.service;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.dto.TeacherQueryDTO;
import com.aiproject.smartcampus.pojo.dto.TeacherUpdateDTO;
import com.aiproject.smartcampus.pojo.po.Teacher;
import com.baomidou.mybatisplus.extension.service.IService;

public interface TeacherService extends IService<Teacher> {
    Result<Teacher> queryTeachersById(TeacherQueryDTO queryDTO);

    Result updateTeacherInfo(TeacherUpdateDTO updateDTO);

    Result getAllClassInfo( String couresId);

    Result getTheMaxUncorrectPoint(String couresId);

//    Result updateTeacherStatus(TeacherStatusDTO statusDTO);

}
