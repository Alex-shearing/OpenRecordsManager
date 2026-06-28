package com.openrecordsmanager.api.config;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public class ConfigValueType<T> {
    public static final ConfigValueType<String> STRING = new ConfigValueType<>(String.class, Function.identity());
    public static final ConfigValueType<Integer> INT = new ConfigValueType<>(Integer.class, Integer::valueOf);
    public static final ConfigValueType<Double> DOUBLE = new ConfigValueType<>(Double.class, Double::valueOf);
    public static final ConfigValueType<Boolean> BOOL = new ConfigValueType<>(Boolean.class, Boolean::valueOf);
    public static final ConfigValueType<UUID> UUID = new ConfigValueType<>(UUID.class, java.util.UUID::fromString);
    public static final ConfigValueType<String[]> STRING_LIST = new ConfigValueType<>(String[].class, s -> s.split(";"));
    public static final ConfigValueType<Integer[]> INT_LIST = new ConfigValueType<>(Integer[].class, s -> Arrays.stream(s.split(";")).mapToInt(Integer::valueOf).boxed().toArray(Integer[]::new));

    public final Class<T> cType;
    private final Function<String, T> converter;

    private ConfigValueType(Class<T> cType, Function<String, T> converter) {
        this.cType = cType;
        this.converter = converter;
    }

    public Optional<T> fromString(String value) {
        try {
            return this.converter.andThen(Optional::of).apply(value);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return this.cType.getSimpleName();
    }
}
