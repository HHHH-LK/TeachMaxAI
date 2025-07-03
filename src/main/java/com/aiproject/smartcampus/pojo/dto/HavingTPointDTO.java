package com.aiproject.smartcampus.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @program: ss
 * @description:带权知识点id
 * @author: lk_hhh
 * @create: 2025-07-03 09:59
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HavingTPointDTO {

    private Integer pointIds;
    private Integer T;


}