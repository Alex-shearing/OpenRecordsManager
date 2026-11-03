package com.openrecordsmanager.relationship;

import com.openrecordsmanager.api.ResourceIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RelationshipTypeRepository extends JpaRepository<RelationshipType, ResourceIdentifier> {
}
