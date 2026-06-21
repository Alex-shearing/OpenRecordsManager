package com.openrecordsmanager.model.repositories;

import com.openrecordsmanager.model.ListElement;
import com.openrecordsmanager.resources.ResourceIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ListElementRepository extends JpaRepository<ListElement, ResourceIdentifier> {
}
