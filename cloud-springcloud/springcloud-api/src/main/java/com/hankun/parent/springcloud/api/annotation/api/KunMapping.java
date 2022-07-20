package com.hankun.parent.springcloud.api.annotation.api;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.Mapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
@RequestMapping
public @interface KunMapping {

    @AliasFor(annotation = RequestMapping.class)
    String[] value() default {};
}
