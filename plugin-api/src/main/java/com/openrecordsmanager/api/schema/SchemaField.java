package com.openrecordsmanager.api.schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.RECORD_COMPONENT)
@Retention(RetentionPolicy.RUNTIME)
public @interface SchemaField {
    String title();

    String description() default "";

    boolean required() default true;

    SchemaFieldFormat format() default SchemaFieldFormat.TEXT;

    int minLength() default -1;

    int maxLength() default -1;

    String pattern() default "";
}
