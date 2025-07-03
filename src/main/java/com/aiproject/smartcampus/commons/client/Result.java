package com.aiproject.smartcampus.commons.client;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @program: SmartCampus
 * @description: 通用返回结果封装
 * @author: lk
 * @create: 2025-05-17 17:01
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    @Schema(description = "响应码，0 表示成功，非 0 表示失败")
    private Integer code;

    @Schema(description = "响应数据")
    private T data;

    @Schema(description = "提示消息")
    private String message;

    /**
     * 操作是否成功
     */
    public boolean isSuccess() {
        return this.code != null && this.code == 0;
    }

    /**
     * 操作成功，无数据返回
     */
    public static <T> Result<T> success() {
        return new Result<>(0, null, "success");
    }

    /**
     * 操作成功，返回数据
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(0, data, "success");
    }

    /**
     * 操作失败，返回错误信息
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(1, null, message);
    }

    /**
     * 自定义返回
     */
    public static <T> Result<T> of(int code, T data, String message) {
        return new Result<>(code, data, message);
    }

    public Result put(T token, T token1) {
        return null;
    }
}
