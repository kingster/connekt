package com.flipkart.connekt.commons.entities;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.METHOD)
@Documented
public @interface EnumTypeHint {
    String value();
}