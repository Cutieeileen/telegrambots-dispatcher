package org.itburger.telegramupdatesdispatcher.annotations;

import org.itburger.telegramupdatesdispatcher.enums.SourceType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AudioHandler {
    SourceType[] sources() default {SourceType.USER};
    boolean accessByUnknownUsers() default false;
    String[] requiredStates() default {};
}
