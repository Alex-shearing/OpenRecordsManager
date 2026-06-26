package com.openrecordsmanager.controllers;

import com.openrecordsmanager.controllers.errors.ApiError;
import com.openrecordsmanager.model.ObjectProperty;
import com.openrecordsmanager.model.Record;
import com.openrecordsmanager.model.RecordType;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.types.ComponentTypes;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/records")
public class RecordController {

    private final ExpressionsService expressions;
    private final DataRepository repository;

    public RecordController(ExpressionsService expressions, DataRepository repository) {
        this.expressions = expressions;
        this.repository = repository;
    }

    @PostMapping()
    public Record newRecord(@RequestBody NewRecordContent input) {
        RecordType type = this.repository.recordTypeRepo.findById(input.type())
                .orElseThrow(() -> ApiError.notFound(ComponentTypes.RECORD_TYPE, input.type()));

        Record record = new Record(UUID.randomUUID(), "tba", type, null);
        input.properties.forEach((identifier, o) -> {
            ObjectProperty<?> property = this.repository.objectPropertyRepo.findById(identifier)
                    .orElseThrow(() -> ApiError.notFound(ComponentTypes.PROPERTY, identifier));
            setProperty(record, property, o);
        });

        return this.repository.recordRepo.saveAndFlush(record);
    }

    private static <K> void setProperty(Record record, ObjectProperty<K> property, Object value) {
        record.setProperty(property, property.type.cast(value));
    }

    public record NewRecordContent(
            ResourceIdentifier type,
            Map<ResourceIdentifier, Object> properties
    ) {
    }


}
