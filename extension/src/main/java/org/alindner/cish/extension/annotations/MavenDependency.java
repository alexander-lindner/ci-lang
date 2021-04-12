package org.alindner.cish.extension.annotations;

import org.alindner.cish.extension.Type;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(MavenDependencies.class)
public @interface MavenDependency {
	String value();
	String name();
	String version();
	Type type() default Type.EQUALS;
}
