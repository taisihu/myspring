package com.hts.spring.framwork.annotation;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HTSAutowired{

    public String getValue() default "";

}
