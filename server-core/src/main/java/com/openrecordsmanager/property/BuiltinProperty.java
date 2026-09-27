package com.openrecordsmanager.property;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an entity field as a builtin column-backed {@link ObjectPropertyHolder} property.
 * Discovered by {@link BuiltinPropertyBinding#scan(Class)}.
 * <p>
 * {@code jsonStored} is inferred from {@code @JdbcTypeCode(SqlTypes.JSON)}.
 * {@code required} is inferred from {@code @Column(nullable = false)}.
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BuiltinProperty {
    /**
     * Builtin property item name (e.g. {@link com.openrecordsmanager.api.builtin.BuiltinPropertyIds#GIVEN_NAME_ID}),
     * resolved via {@link com.openrecordsmanager.api.builtin.BuiltinPropertyIds#id(String)}.
     */
    String value();

    boolean defaultSearch() default false;

    /**
     * When true, {@link BuiltinPropertyBinding#set} rejects writes.
     */
    boolean readOnly() default false;
}
