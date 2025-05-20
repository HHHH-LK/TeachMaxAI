package com.aiproject.smartcampus.pojo.bo.handlerentity;

import lombok.Data;

/**
 * @program: SmartCampus
 * @description: 责任链响应体
 * @author: lk
 * @create: 2025-05-19 17:22
 **/

@Data
public class HandlerResponse {

    protected Boolean isSuccess;
    protected String errorMsg;

}
