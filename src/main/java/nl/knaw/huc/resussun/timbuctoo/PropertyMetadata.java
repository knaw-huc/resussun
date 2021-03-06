package nl.knaw.huc.resussun.timbuctoo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PropertyMetadata {
  private final String uri;
  private final String name;
  private final boolean isList;
  private final boolean isValueType;
  private final boolean isIncoming;
  private final List<String> referencedCollectionIds;

  @JsonCreator
  public PropertyMetadata(@JsonProperty("uri") String uri,
                          @JsonProperty("name") String name,
                          @JsonProperty("isList") boolean isList,
                          @JsonProperty("isValueType") boolean isValueType,
                          @JsonProperty("isInverse") boolean isIncoming,
                          @JsonProperty("referencedCollections") ReferencedCollections referencedCollections
  ) {
    this.uri = uri;
    this.name = name;
    this.isList = isList;
    this.isValueType = isValueType;
    this.isIncoming = isIncoming;
    this.referencedCollectionIds = referencedCollections.getItems();
  }

  public PropertyMetadata(String uri, String name, boolean isList, boolean isValueType, boolean isIncoming) {

    this.uri = uri;
    this.name = name;
    this.isList = isList;
    this.isValueType = isValueType;
    this.isIncoming = isIncoming;
    this.referencedCollectionIds = new ArrayList<>();
  }

  public String getName() {
    return name;
  }

  public boolean isList() {
    return isList;
  }

  public boolean isValueType() {
    return isValueType;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    PropertyMetadata that = (PropertyMetadata) obj;
    return isList == that.isList &&
      isValueType == that.isValueType &&
      Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, isList, isValueType);
  }

  public String getUri() {
    return uri;
  }

  public boolean isOutgoing() {
    return !isIncoming;
  }

  public List<String> getReferencedCollectionIds() {
    return referencedCollectionIds;
  }
}
