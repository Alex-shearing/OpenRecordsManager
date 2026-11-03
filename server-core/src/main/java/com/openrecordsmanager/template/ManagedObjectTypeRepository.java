package com.openrecordsmanager.template;

import com.openrecordsmanager.api.ResourceIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ManagedObjectTypeRepository
        extends JpaRepository<ManagedObjectType, ResourceIdentifier> {
}
