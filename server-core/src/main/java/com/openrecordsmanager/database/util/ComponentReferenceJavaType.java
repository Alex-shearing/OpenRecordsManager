package com.openrecordsmanager.database.util;

import com.openrecordsmanager.api.ComponentReference;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.jspecify.annotations.Nullable;

public class ComponentReferenceJavaType extends AbstractClassJavaType<ComponentReference<?>> {

    @SuppressWarnings("unchecked")
    public ComponentReferenceJavaType() {
        super((Class<ComponentReference<?>>) (Class<?>) ComponentReference.class, ImmutableMutabilityPlan.instance());
    }

    @Override
    public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
        return VarcharJdbcType.INSTANCE;
    }

    @Override
    public @Nullable String toString(@Nullable ComponentReference value) {
        return value != null ? value.toString() : null;
    }

    @Override
    public @Nullable ComponentReference<?> fromString(@Nullable CharSequence string) {
        return string != null ? ComponentReference.valueOf(string.toString()) : null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> @Nullable X unwrap(@Nullable ComponentReference<?> value, Class<X> type, WrapperOptions options) {
        if (value == null) return null;
        if (type.isAssignableFrom(String.class)) return (X) value.toString();
        throw unknownUnwrap(type);
    }

    @Override
    public <X> @Nullable ComponentReference<?> wrap(@Nullable X value, WrapperOptions options) {
        return switch (value) {
            case null -> null;
            case ComponentReference<?> identifier -> identifier;
            case String str -> ComponentReference.valueOf(str);
            default -> throw unknownWrap(value.getClass());
        };
    }
}