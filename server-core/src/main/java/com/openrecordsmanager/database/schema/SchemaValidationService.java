package com.openrecordsmanager.database.schema;

import com.openrecordsmanager.database.dto.SchemaValidationResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchemaValidationService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public SchemaValidationResponse validate() {
        try {
            Session session = this.entityManager.unwrap(Session.class);
            SessionFactory sessionFactory = session.getSessionFactory();
            sessionFactory.getSchemaManager().validateMappedObjects();

            return new SchemaValidationResponse(true, null);
        } catch (SchemaManagementException e) {
            return new SchemaValidationResponse(false, e.getMessage());
        }
    }
}
