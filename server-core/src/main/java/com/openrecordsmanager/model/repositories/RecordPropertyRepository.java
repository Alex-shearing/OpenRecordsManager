package com.openrecordsmanager.model.repositories;

import com.openrecordsmanager.model.ObjectProperty;
import com.openrecordsmanager.model.util.DbResourceIdentifier;
import com.openrecordsmanager.resources.ResourceIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecordPropertyRepository extends JpaRepository<ObjectProperty, DbResourceIdentifier> {
    default Optional<ObjectProperty> findById(ResourceIdentifier dbResourceIdentifier) {
        return this.findById(new DbResourceIdentifier(dbResourceIdentifier));
    }
}
