package com.openrecordsmanager.database.util;

import com.openrecordsmanager.api.ResourceIdentifier;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.jspecify.annotations.Nullable;

public class ResourceIdentifierJavaType extends AbstractClassJavaType<ResourceIdentifier> {

    public ResourceIdentifierJavaType() {
        super(ResourceIdentifier.class, ImmutableMutabilityPlan.instance());
    }

    @Override
    public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
        return VarcharJdbcType.INSTANCE;
    }

    @Override
    public @Nullable String toString(@Nullable ResourceIdentifier value) {
        return value != null ? value.toString() : null;
    }

    @Override
    public @Nullable ResourceIdentifier fromString(@Nullable CharSequence string) {
        return string != null ? ResourceIdentifier.valueOf(string.toString()) : null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> @Nullable X unwrap(@Nullable ResourceIdentifier value, Class<X> type, WrapperOptions options) {
        if (value == null) return null;
        if (type.isAssignableFrom(String.class)) return (X) value.toString();
        throw unknownUnwrap(type);
    }

    @Override
    public <X> @Nullable ResourceIdentifier wrap(@Nullable X value, WrapperOptions options) {
        return switch (value) {
            case null -> null;
            case ResourceIdentifier identifier -> identifier;
            case String str -> ResourceIdentifier.valueOf(str);
            default -> throw unknownWrap(value.getClass());
        };
    }
}