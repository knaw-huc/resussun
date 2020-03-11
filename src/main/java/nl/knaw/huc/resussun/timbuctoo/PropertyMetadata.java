package nl.knaw.huc.resussun.timbuctoo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class PropertyMetadata {
  private final String name;
  private final boolean isList;
  private final boolean isValueType;

  @JsonCreator
  public PropertyMetadata(@JsonProperty("name") String name,
                          @JsonProperty("isList") boolean isList,
                          @JsonProperty("isValueType") boolean isValueType
  ) {
    this.name = name;
    this.isList = isList;
    this.isValueType = isValueType;
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
}
