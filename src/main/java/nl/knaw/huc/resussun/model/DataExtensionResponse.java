package nl.knaw.huc.resussun.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DataExtensionResponse {

  @JsonProperty
  private final List<DataExtensionResponsePropertyMetadata> meta;
  @JsonProperty
  private final Map<String, Map<String, List<? extends PropertyValue>>> rows;

  public DataExtensionResponse(List<DataExtensionResponsePropertyMetadata> meta,
                               Map<String, Map<String, List<? extends PropertyValue>>> rows) {
    this.meta = meta;
    this.rows = rows;
  }

  public static DataExtensionResponsePropertyMetadata createPropertyMetadata(String id, String name,
                                                                             Map<String, String> settings) {
    return new DataExtensionResponsePropertyMetadata(id, name, settings, null);
  }

  public static DataExtensionResponsePropertyMetadata createPropertyMetadata(String id, String name,
                                                                             Map<String, String> settings, Type type) {
    return new DataExtensionResponsePropertyMetadata(id, name, settings, type);
  }

  public static class Type {
    @JsonProperty
    private final String name;
    @JsonProperty
    private final String id;

    public Type(String name, String id) {
      this.name = name;
      this.id = id;
    }
  }

  public interface PropertyValue {

  }

  public static class DataExtensionResponsePropertyMetadata {
    private final String id;
    private final String name;
    private final Map<String, String> settings;
    private final Type type;

    public DataExtensionResponsePropertyMetadata(String id, String name, Map<String, String> settings, Type type) {
      this.id = id;
      this.name = name;
      this.settings = settings;
      this.type = type;
    }

    public String getId() {
      return id;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, String> getSettings() {
      return settings;
    }

    public String getName() {
      return name;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Type getType() {
      return type;
    }
  }

  public static class LiteralPropertyValue implements PropertyValue {
    @JsonProperty
    private String str;

    public LiteralPropertyValue(String str) {
      this.str = str;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      LiteralPropertyValue that = (LiteralPropertyValue) obj;
      return Objects.equals(str, that.str);
    }

    @Override
    public int hashCode() {
      return Objects.hash(str);
    }

    @Override
    public String toString() {
      return "LiteralPropertyValue{" +
          "str='" + str + '\'' +
          '}';
    }
  }

  public static class ReferencePropertyValue implements PropertyValue {
    @JsonProperty
    private final String id;
    @JsonProperty
    private final String name;

    public ReferencePropertyValue(String id, String name) {
      this.id = id;
      this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      ReferencePropertyValue that = (ReferencePropertyValue) obj;
      return Objects.equals(id, that.id) &&
          Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, name);
    }

    @Override
    public String toString() {
      return "ReferencePropertyValue{" +
          "id='" + id + '\'' +
          ", name='" + name + '\'' +
          '}';
    }
  }
}
