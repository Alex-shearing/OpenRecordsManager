package com.openrecordsmanager.model.repositories;

import org.springframework.stereotype.Service;

@Service
public class DataRepository {
    public final AuthProviderRepository authProviderRepo;
    public final ConfigRepository configRepo;
    public final ListElementRepository listElementRepo;
    public final ListTypeRepository listTypeRepo;
    public final ObjectPropertyRepository objectPropertyRepo;
    public final RecordTypeRepository recordTypeRepo;
    public final RecordTypePropertyRepository recordTypePropertyRepo;

    public DataRepository(AuthProviderRepository authProviderRepo, ConfigRepository configRepo, ListElementRepository listElementRepo, ListTypeRepository listTypeRepo, ObjectPropertyRepository objectPropertyRepo, RecordTypeRepository recordTypeRepo, RecordTypePropertyRepository recordTypePropertyRepo) {
        this.authProviderRepo = authProviderRepo;
        this.configRepo = configRepo;
        this.listElementRepo = listElementRepo;
        this.listTypeRepo = listTypeRepo;
        this.objectPropertyRepo = objectPropertyRepo;
        this.recordTypeRepo = recordTypeRepo;
        this.recordTypePropertyRepo = recordTypePropertyRepo;
    }
}
