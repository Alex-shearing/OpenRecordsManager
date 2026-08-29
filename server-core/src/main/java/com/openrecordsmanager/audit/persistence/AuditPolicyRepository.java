package com.openrecordsmanager.audit.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditPolicyRepository extends JpaRepository<AuditPolicyEntity, AuditPolicyId> {
}
