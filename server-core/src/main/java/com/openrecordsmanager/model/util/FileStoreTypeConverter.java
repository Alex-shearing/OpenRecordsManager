package com.openrecordsmanager.model.util;

import com.openrecordsmanager.api.filestore.FileStoreType;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.types.ComponentTypes;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class FileStoreTypeConverter implements AttributeConverter<FileStoreType<?>, String> {
    private final ComponentCatalog catalog;

    public FileStoreTypeConverter(ComponentCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public String convertToDatabaseColumn(FileStoreType<?> fileStoreType) {
        ResourceIdentifier id = this.catalog.getId(ComponentTypes.FILE_STORE_TYPE, fileStoreType);
        if (id == null) {
            return null;
        }

        return id.toString();
    }

    @Override
    public FileStoreType<?> convertToEntityAttribute(String s) {
        return this.catalog.getComponent(ComponentTypes.FILE_STORE_TYPE, new ResourceIdentifier(s)).orElse(null);
    }
}
