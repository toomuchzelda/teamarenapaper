package me.toomuchzelda.teamarenapaper.utils;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.METHOD, ElementType.TYPE_USE})
public @interface ConfigOptional {
}
