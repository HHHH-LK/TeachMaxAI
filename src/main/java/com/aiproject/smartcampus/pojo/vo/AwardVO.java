package com.aiproject.smartcampus.pojo.vo;

import com.aiproject.smartcampus.pojo.po.Item;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @program: TeacherMaxAI
 * @description: 获取奖励
 * @author: lk_hhh
 * @create: 2025-08-13 10:24
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AwardVO {

    private String Exp;
    private List<Item> item;


}