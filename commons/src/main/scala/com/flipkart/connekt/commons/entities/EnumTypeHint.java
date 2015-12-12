package com.flipkart.connekt.commons.entities;

import java.lang.annotation.*;

/**
 * Created by kinshuk.bairagi on 09/02/15.
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.METHOD)
@Documented
public @interface EnumTypeHint {
    String value();
}