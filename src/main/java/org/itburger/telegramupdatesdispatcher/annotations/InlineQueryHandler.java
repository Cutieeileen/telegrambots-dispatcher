package org.itburger.telegramupdatesdispatcher.annotations;

import org.intellij.lang.annotations.Language;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InlineQueryHandler {
    @Language("RegExp")
    String regex() default "";
    boolean startsWith() default false;
    boolean accessByUnknownUsers() default false;
    String[] requiredStates() default {};
}

