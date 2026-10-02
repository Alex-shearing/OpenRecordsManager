package com.openrecordsmanager.location.relationship;

import com.openrecordsmanager.api.ResourceIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRelationshipTypeRepository extends JpaRepository<LocationRelationshipType, ResourceIdentifier> {
}
