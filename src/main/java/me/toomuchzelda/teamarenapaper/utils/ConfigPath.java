package me.toomuchzelda.teamarenapaper.utils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConfigPath {
	String value();
}
