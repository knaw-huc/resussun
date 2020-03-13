package nl.knaw.huc.resussun.dataextension;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DataExtensionRequest {
  private final List<String> entityIds;
  private final List<Property> properties;

  @JsonCreator
  public DataExtensionRequest(@JsonProperty("ids") List<String> entityIds,
                              @JsonProperty("properties") List<Property> properties) {
    this.entityIds = entityIds;
    this.properties = properties;
  }

  public List<String> getEntityIds() {
    return entityIds;
  }

  public List<Property> getProperties() {
    return properties;
  }

  public static class Property {
    private final String id;
    private final Map<String, String> settings;

    @JsonCreator
    public Property(@JsonProperty("id") String id, @JsonProperty("settings") Map<String, String> settings) {
      this.id = id;
      this.settings = settings;
    }

    public String getId() {
      return id;
    }

    public Optional<Map<String, String>> getSettings() {
      return Optional.ofNullable(settings);
    }
  }

}
