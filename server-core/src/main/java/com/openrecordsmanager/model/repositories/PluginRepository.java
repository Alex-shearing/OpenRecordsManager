package com.openrecordsmanager.model.repositories;

import com.openrecordsmanager.model.Plugin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PluginRepository extends JpaRepository<Plugin, String> {
}
