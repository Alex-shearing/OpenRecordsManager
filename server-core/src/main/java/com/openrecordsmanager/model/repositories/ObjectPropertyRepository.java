package com.openrecordsmanager.model.repositories;

import com.openrecordsmanager.model.ObjectProperty;
import com.openrecordsmanager.api.ResourceIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ObjectPropertyRepository extends JpaRepository<ObjectProperty<?>, ResourceIdentifier> {
}
