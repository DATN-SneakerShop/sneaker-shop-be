package com.sneakershop.backend.audit;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditAction {
    String module();
    String action();
    String entity() default "";
    String description() default ""; // Cho phép nhập chi tiết thêm
}