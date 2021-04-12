package org.alindner.cish.extension.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(CishDependencies.class)
public @interface CishDependency {
	String value() default "";
}