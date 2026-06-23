package com.openrecordsmanager.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "record_type_property")
public class RecordTypeProperty<T> {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    public int id;

    @JoinColumn(name = "record_type_id")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    public RecordType recordType;

    @JoinColumn(name = "property_id")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    public ObjectProperty<?> property;

    @Column()
    @JdbcTypeCode(SqlTypes.JSON)
    public T defaultValue;

    @Deprecated
    protected RecordTypeProperty() {
    }

    public RecordTypeProperty(RecordType type, ObjectProperty<T> property, T defaultValue) {
        this.recordType = type;
        this.property = property;
        this.defaultValue = defaultValue;
    }
}
