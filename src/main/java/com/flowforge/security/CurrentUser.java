package com.flowforge.security;

import java.lang.annotation.*;

/**
 * Annotation to inject the current authenticated user's ID into controller methods.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
