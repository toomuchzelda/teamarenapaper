package me.toomuchzelda.teamarenapaper.utils;

import java.lang.annotation.*;

/**
 * Indicates that a field or record component is optional, and will default to null or 0
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.METHOD, ElementType.TYPE_USE})
public @interface ConfigOptional {
}
