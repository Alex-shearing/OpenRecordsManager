package com.openrecordsmanager.list;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.template.RegisteredComponent;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "list_type")
@DiscriminatorValue("list")
@PrimaryKeyJoinColumn(name = "id")
@SuppressWarnings("CanBeFinal")
public class ListType extends RegisteredComponent {
    @OneToMany(mappedBy = "parent")
    @OrderBy("elementIndex ASC")
    private List<ListElement> children = new ArrayList<>();

    @Deprecated
    protected ListType() {
    }

    public ListType(ResourceIdentifier id) {
        super(id);
    }

    public List<ListElement> getChildren() {
        return this.children;
    }

    @Override
    public ComponentType<?> getComponentType() {
        return ComponentTypes.LIST;
    }
}
