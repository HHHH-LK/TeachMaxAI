package com.aiproject.smartcampus.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @program: SmartCampus
 * @description: 图节点
 * @author: lk
 * @create: 2025-05-28 21:47
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
//意图识别节点
public class Node {

    private String intent;
    private String handler;

}
