package top.rymc.phira.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Plugin {
    String id();
    String name() default "";
    String version();
    String[] authors() default {};
    String[] dependencies() default {};
    String description() default "";
}
