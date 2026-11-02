package com.velox.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Put on an {@link AuthUser} controller parameter to get the signed-in account. Without a valid token the request
 * fails with 401 before the controller runs; use {@code required = false} for endpoints open to visitors.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
    boolean required() default true;
}
