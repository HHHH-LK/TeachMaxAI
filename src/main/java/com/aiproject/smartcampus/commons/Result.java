package com.aiproject.smartcampus.commons;

import lombok.Data;

/**
 * @program: SmartCampus
 * @description: 通用返回符
 * @author: lk
 * @create: 2025-05-17 17:01
 **/

@Data
public class  Result {

    private Integer code;
    private Object data;
    private String message;

    public Result(Integer code , Object data, String message) {}

    public static Result success(Object data){return new Result(0,data,"success");}
    public static Result success(){return new Result(0,null,"success");}
    public static Result error(String message){return new Result(1,null,message);}

}
