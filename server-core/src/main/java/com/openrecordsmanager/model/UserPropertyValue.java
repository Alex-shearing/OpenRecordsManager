package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "user_property_value")
public class UserPropertyValue<T> {
    @Id
    @JsonProperty
    public UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    public User user;

    @ManyToOne(targetEntity = ObjectProperty.class)
    @JoinColumn(name = "property_id")
    public ObjectProperty<T> property;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    @JsonProperty
    public T value;

    public UserPropertyValue() {
    }

    public UserPropertyValue(User user, ObjectProperty<T> property, T value) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.property = property;
        this.value = value;
    }
}
