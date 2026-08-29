package com.openrecordsmanager.audit;

import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.list.ListElement;
import com.openrecordsmanager.record.Record;
import com.openrecordsmanager.record.RecordRevision;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class AuditEventDescriptions {

    private AuditEventDescriptions() {
    }

    public static String summary(AuditOperation operation, AuditEntityType targetType, String targetId) {
        String typeLabel = targetType.key();
        return switch (operation) {
            case CREATE -> "Created " + typeLabel + " " + targetId;
            case READ -> {
                String prefix = targetId.equals(AuditService.COLLECTION_TARGET_ID) ? "Listed all" : "Read";
                yield String.format("%s %s %s", prefix, typeLabel, targetId);
            }
            case UPDATE -> "Updated " + typeLabel + " " + targetId;
            case DELETE -> "Deleted " + typeLabel + " " + targetId;
            case ACTION -> "Action ran on " + typeLabel + " " + targetId;
        };
    }

    public static List<AuditRelationship> forRecord(Record record) {
        return List.of(new AuditRelationship(AuditEntityType.RECORD_TYPE, record.getType().id.toString(), "type"));
    }

    public static List<AuditRelationship> forRecordRevision(RecordRevision revision) {
        return List.of(new AuditRelationship(AuditEntityType.RECORD, revision.record.getId().toString(), "record"));
    }

    public static List<AuditRelationship> forListElement(ListElement element) {
        return List.of(new AuditRelationship(AuditEntityType.LIST, element.getParent().getId().toString(), "parent"));
    }

    public static List<AuditPropertyChange> singleChange(
            String field,
            @Nullable Object oldValue,
            @Nullable Object newValue
    ) {
        return List.of(new AuditPropertyChange(field, oldValue, newValue));
    }
}
