package com.openrecordsmanager.list;

import com.openrecordsmanager.api.ResourceIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ListTypeRepository extends JpaRepository<ListType, ResourceIdentifier> {
}
