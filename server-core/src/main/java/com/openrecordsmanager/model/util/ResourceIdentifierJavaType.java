package com.openrecordsmanager.model.util;

import com.openrecordsmanager.api.ResourceIdentifier;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;

public class ResourceIdentifierJavaType extends AbstractClassJavaType<ResourceIdentifier> {

    public ResourceIdentifierJavaType() {
        super(ResourceIdentifier.class, ImmutableMutabilityPlan.instance());
    }

    @Override
    public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
        return VarcharJdbcType.INSTANCE;
    }

    @Override
    public String toString(ResourceIdentifier value) {
        return value.toString();
    }

    @Override
    public ResourceIdentifier fromString(CharSequence string) {
        return ResourceIdentifier.valueOf(string.toString());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> X unwrap(ResourceIdentifier value, Class<X> type, WrapperOptions options) {
        if (type.isAssignableFrom(String.class)) return (X) value.toString();
        throw unknownUnwrap(type);
    }

    @Override
    public <X> ResourceIdentifier wrap(X value, WrapperOptions options) {
        return switch (value) {
            case ResourceIdentifier identifier -> identifier;
            case String str -> ResourceIdentifier.valueOf(str);
            default -> throw unknownWrap(value.getClass());
        };
    }
}