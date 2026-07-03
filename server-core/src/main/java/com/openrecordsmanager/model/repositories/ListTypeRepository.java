package com.openrecordsmanager.model.repositories;

import com.openrecordsmanager.model.ListType;
import com.openrecordsmanager.api.ResourceIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ListTypeRepository extends JpaRepository<ListType, ResourceIdentifier> {
}
