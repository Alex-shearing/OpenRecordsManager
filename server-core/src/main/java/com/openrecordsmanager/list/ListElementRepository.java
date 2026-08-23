package com.openrecordsmanager.list;

import com.openrecordsmanager.api.ResourceIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface ListElementRepository extends JpaRepository<ListElement, ResourceIdentifier> {
    @Query("SELECT DISTINCT s FROM ListElement s LEFT JOIN s.aliases a WHERE s.parent = :parent AND (s.name LIKE CONCAT('%', :search, '%') OR a LIKE CONCAT('%', :search, '%'))")
    Set<ListElement> searchNameAndAlias(@Param("parent") ListType parent, @Param("search") String search);

    @Query("SELECT s FROM ListElement s WHERE s.parent.id = :parent AND s.id = :id")
    Optional<ListElement> getElement(@Param("parent") ResourceIdentifier parent, @Param("id") ResourceIdentifier id);
}
