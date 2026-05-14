package com.ma.grecode.exception;

import com.ma.grecode.utils.AjaxResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public AjaxResult<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return AjaxResult.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public AjaxResult<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        log.warn("参数校验失败: {}", message);
        return AjaxResult.error(400, message);
    }

    @ExceptionHandler(BindException.class)
    public AjaxResult<?> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数绑定失败");
        log.warn("参数绑定失败: {}", message);
        return AjaxResult.error(400, message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public AjaxResult<?> handleIllegalArgumentException(IllegalArgumentException e) {
        String raw = e.getMessage();
        log.warn("非法参数: {}", raw);
        // Spring 在控制器参数未显式命名且 class 未带 -parameters 时常抛出英文长句；业务层避免把该句原样弹给前端
        if (raw != null && raw.contains("parameter name information not available via reflection")) {
            return AjaxResult.error(400, "请求参数无法解析，请重启后端或重新编译后再试（需启用编译参数名保留）。");
        }
        return AjaxResult.error(400, raw != null ? raw : "非法参数");
    }

    @ExceptionHandler(Exception.class)
    public AjaxResult<?> handleException(Exception e) {
        log.error("系统异常", e);
        return AjaxResult.error(500, "系统内部错误，请稍后重试");
    }
}
