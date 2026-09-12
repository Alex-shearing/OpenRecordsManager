package com.openrecordsmanager.api.template.list;

import com.openrecordsmanager.api.ResourceIdentifier;

public interface IListElement {
    ResourceIdentifier getId();

    int index();
}
