package com.openrecordsmanager.list;

import com.openrecordsmanager.api.ResourceIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ListElementRepository extends JpaRepository<ListElement, ResourceIdentifier> {
    @Query("SELECT s FROM ListElement s WHERE s.parent.id = :parent AND s.id = :id")
    Optional<ListElement> getElement(@Param("parent") ResourceIdentifier parent, @Param("id") ResourceIdentifier id);
}
