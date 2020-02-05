package nl.knaw.huc.resussun.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CollectionMetadata {
    @JsonProperty
    private String name;

    @JsonProperty
    private boolean isList;

    @JsonProperty
    private boolean isValueType;

    public String getName() {
        return name;
    }

    public boolean isList() {
        return isList;
    }

    public boolean isValueType() {
        return isValueType;
    }
}
