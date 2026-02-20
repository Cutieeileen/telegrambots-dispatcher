package org.itburger.telegramupdatesdispatcher.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * WORK IN PROGRESS
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BusinessCallbackQueryHandler {
    String value() default "";
    String regex() default "";
    boolean startsWith() default false;
    boolean accessByUnknownUsers() default false;
    String[] requiredStates() default {};
}
