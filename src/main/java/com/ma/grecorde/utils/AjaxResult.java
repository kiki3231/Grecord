package com.ma.grecorde.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ma.grecorde.constant.HttpStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;

@ApiModel("通用返回结果")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AjaxResult<T> {


    @ApiModelProperty("状态码")
    private Integer code;

    @ApiModelProperty("提示信息")
    private String msg;

    @ApiModelProperty("数据对象")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    public AjaxResult() {}

    public AjaxResult(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public AjaxResult(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // 工厂方法
    public static <T> AjaxResult<T> success() {
        return success("操作成功", null);
    }

    public static <T> AjaxResult<T> success(T data) {
        return success("操作成功", data);
    }

    public static <T> AjaxResult<T> success(String msg) {
        return success(msg, null);
    }

    public static <T> AjaxResult<T> success(String msg, T data) {
        return new AjaxResult<>(HttpStatus.SUCCESS, msg, data);
    }

    public static <T> AjaxResult<T> warn(String msg) {
        return new AjaxResult<>(HttpStatus.WARN, msg, null);
    }

    public static <T> AjaxResult<T> warn(String msg, T data) {
        return new AjaxResult<>(HttpStatus.WARN, msg, data);
    }

    public static <T> AjaxResult<T> error() {
        return error("操作失败", null);
    }

    public static <T> AjaxResult<T> error(String msg) {
        return error(msg, null);
    }

    public static <T> AjaxResult<T> error(String msg, T data) {
        return new AjaxResult<>(HttpStatus.ERROR, msg, data);
    }

    public static <T> AjaxResult<T> error(int code, String msg) {
        return new AjaxResult<>(code, msg, null);
    }

    // 状态判断（避免被序列化）
    @JsonIgnore
    public boolean isSuccess() { return Objects.equals(HttpStatus.SUCCESS, this.code); }
    @JsonIgnore
    public boolean isWarn() { return Objects.equals(HttpStatus.WARN, this.code); }
    @JsonIgnore
    public boolean isError() { return Objects.equals(HttpStatus.ERROR, this.code); }

    // getter/setter
    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

}
