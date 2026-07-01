package com.openrecordsmanager.model.repositories;

import com.openrecordsmanager.model.PersistedPlugin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PluginRepository extends JpaRepository<PersistedPlugin, String> {
}
