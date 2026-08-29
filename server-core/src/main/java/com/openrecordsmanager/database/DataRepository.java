package com.openrecordsmanager.database;

import com.openrecordsmanager.audit.persistence.AuditEventRepository;
import com.openrecordsmanager.audit.persistence.AuditPolicyRepository;
import com.openrecordsmanager.auth.AuthProviderRepository;
import com.openrecordsmanager.auth.entity.AuthTokenRepository;
import com.openrecordsmanager.config.ConfigRepository;
import com.openrecordsmanager.filestore.middleware.MiddlewareRepository;
import com.openrecordsmanager.filestore.store.FileStoreRepository;
import com.openrecordsmanager.list.ListElementRepository;
import com.openrecordsmanager.list.ListTypeRepository;
import com.openrecordsmanager.property.ObjectPropertyRepository;
import com.openrecordsmanager.record.RecordRepository;
import com.openrecordsmanager.recordtype.RecordTypeRepository;
import com.openrecordsmanager.user.UserRepository;
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
    public final RecordTypeRepository recordTypeRepo;
    public final RecordRepository recordRepo;
    public final FileStoreRepository fileStoreRepo;
    public final MiddlewareRepository fileStoreMiddlewareRepo;
    public final UserRepository userRepo;
    public final AuthTokenRepository authTokenRepo;

    public DataRepository(
            AuditEventRepository auditEventRepo,
            AuditPolicyRepository auditPolicyRepo,
            AuthProviderRepository authProviderRepo,
            ConfigRepository configRepo,
            ListElementRepository listElementRepo,
            ListTypeRepository listTypeRepo,
            ObjectPropertyRepository objectPropertyRepo,
            RecordTypeRepository recordTypeRepo,
            RecordRepository recordRepo,
            FileStoreRepository fileStoreRepo,
            MiddlewareRepository fileStoreMiddlewareRepo,
            UserRepository userRepo,
            AuthTokenRepository authTokenRepo
    ) {
        this.auditEventRepo = auditEventRepo;
        this.auditPolicyRepo = auditPolicyRepo;
        this.authProviderRepo = authProviderRepo;
        this.configRepo = configRepo;
        this.listElementRepo = listElementRepo;
        this.listTypeRepo = listTypeRepo;
        this.objectPropertyRepo = objectPropertyRepo;
        this.recordTypeRepo = recordTypeRepo;
        this.recordRepo = recordRepo;
        this.fileStoreRepo = fileStoreRepo;
        this.fileStoreMiddlewareRepo = fileStoreMiddlewareRepo;
        this.userRepo = userRepo;
        this.authTokenRepo = authTokenRepo;
    }
}
