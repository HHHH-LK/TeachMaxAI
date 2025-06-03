package com.aiproject.smartcampus.pojo.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @program: SmartCampus
 * @description: 边（依赖关系）
 * @author: lk
 * @create: 2025-05-28 21:49
 **/


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Side {
    //被依赖
    private String from;
    //依赖from
    private String to;

}
