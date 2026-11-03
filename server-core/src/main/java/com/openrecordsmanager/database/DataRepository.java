package com.openrecordsmanager.database;

import com.openrecordsmanager.audit.persistence.AuditEventRepository;
import com.openrecordsmanager.audit.persistence.AuditPolicyRepository;
import com.openrecordsmanager.auth.AuthProviderRepository;
import com.openrecordsmanager.config.ConfigRepository;
import com.openrecordsmanager.property.ManagedObjectRepository;
import com.openrecordsmanager.filestore.middleware.MiddlewareRepository;
import com.openrecordsmanager.filestore.store.FileStoreRepository;
import com.openrecordsmanager.list.ListElementRepository;
import com.openrecordsmanager.list.ListTypeRepository;
import com.openrecordsmanager.location.LocationRepository;
import com.openrecordsmanager.location.group.GroupRepository;
import com.openrecordsmanager.location.type.LocationTypeRepository;
import com.openrecordsmanager.location.user.UserRepository;
import com.openrecordsmanager.plugin.PluginRepository;
import com.openrecordsmanager.property.ObjectPropertyRepository;
import com.openrecordsmanager.record.RecordRepository;
import com.openrecordsmanager.record.type.RecordTypeRepository;
import com.openrecordsmanager.relationship.RelationshipRepository;
import com.openrecordsmanager.relationship.RelationshipTypeRepository;
import com.openrecordsmanager.template.ManagedObjectTypeRepository;
import org.springframework.stereotype.Service;

@Service
public class DataRepository {
    public final AuditEventRepository auditEventRepo;
    public final AuditPolicyRepository auditPolicyRepo;
    public final AuthProviderRepository authProviderRepo;
    public final ConfigRepository configRepo;
    public final ListElementRepository listElementRepo;
    public final ListTypeRepository listTypeRepo;
    public final ObjectPropertyRepository objectPropertyRepo;
    public final ManagedObjectTypeRepository managedObjectTypeRepo;
    public final RecordTypeRepository recordTypeRepo;
    public final RecordRepository recordRepo;
    public final ManagedObjectRepository managedObjectRepo;
    public final FileStoreRepository fileStoreRepo;
    public final MiddlewareRepository fileStoreMiddlewareRepo;
    public final PluginRepository pluginRepo;
    public final UserRepository userRepo;
    public final GroupRepository groupRepo;
    public final LocationRepository locationRepo;
    public final LocationTypeRepository locationTypeRepo;
    public final RelationshipTypeRepository relationshipTypeRepo;
    public final RelationshipRepository relationshipRepo;

    public DataRepository(
            AuditEventRepository auditEventRepo,
            AuditPolicyRepository auditPolicyRepo,
            AuthProviderRepository authProviderRepo,
            ConfigRepository configRepo,
            ListElementRepository listElementRepo,
            ListTypeRepository listTypeRepo,
            ObjectPropertyRepository objectPropertyRepo,
            ManagedObjectTypeRepository managedObjectTypeRepo,
            RecordTypeRepository recordTypeRepo,
            RecordRepository recordRepo,
            ManagedObjectRepository managedObjectRepo,
            FileStoreRepository fileStoreRepo,
            MiddlewareRepository fileStoreMiddlewareRepo,
            PluginRepository pluginRepo,
            UserRepository userRepo,
            GroupRepository groupRepo,
            LocationRepository locationRepo,
            LocationTypeRepository locationTypeRepo,
            RelationshipTypeRepository relationshipTypeRepo,
            RelationshipRepository relationshipRepo
    ) {
        this.auditEventRepo = auditEventRepo;
        this.auditPolicyRepo = auditPolicyRepo;
        this.authProviderRepo = authProviderRepo;
        this.configRepo = configRepo;
        this.listElementRepo = listElementRepo;
        this.listTypeRepo = listTypeRepo;
        this.objectPropertyRepo = objectPropertyRepo;
        this.managedObjectTypeRepo = managedObjectTypeRepo;
        this.recordTypeRepo = recordTypeRepo;
        this.recordRepo = recordRepo;
        this.managedObjectRepo = managedObjectRepo;
        this.fileStoreRepo = fileStoreRepo;
        this.fileStoreMiddlewareRepo = fileStoreMiddlewareRepo;
        this.pluginRepo = pluginRepo;
        this.userRepo = userRepo;
        this.groupRepo = groupRepo;
        this.locationRepo = locationRepo;
        this.locationTypeRepo = locationTypeRepo;
        this.relationshipTypeRepo = relationshipTypeRepo;
        this.relationshipRepo = relationshipRepo;
    }
}
