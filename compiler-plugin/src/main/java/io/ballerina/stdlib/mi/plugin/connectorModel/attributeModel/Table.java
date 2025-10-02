package io.ballerina.stdlib.mi.plugin.connectorModel.attributeModel;

public class Table extends Element {
    private final String name;
    private final String displayName;
    private final String title;
    private final String description;

    public Table (String name, String displayName, String title, String description) {
        this.name = name;
        this.displayName = displayName;
        this.title = title;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
