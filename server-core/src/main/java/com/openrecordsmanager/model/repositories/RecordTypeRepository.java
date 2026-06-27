package com.openrecordsmanager.model.repositories;

import com.openrecordsmanager.model.RecordType;
import com.openrecordsmanager.resources.ResourceIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecordTypeRepository extends JpaRepository<RecordType, ResourceIdentifier> {
    @Query("SELECT r.id FROM RecordType r")
    List<ResourceIdentifier> findAllIds();
}
