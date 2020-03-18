package nl.knaw.huc.resussun.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import nl.knaw.huc.resussun.resources.DataExtensionPropertyProposalResource;

import java.util.List;

public class PropertyProposal {
  @JsonProperty
  private String type;
  @JsonProperty
  private List<Property> properties;

  public PropertyProposal(String type, List<Property> properties) {

    this.type = type;
    this.properties = properties;
  }

  public static class Property {
    @JsonProperty
    private final String name;
    @JsonProperty
    private final String id;

    public Property(String id, String name) {

      this.name = name;
      this.id = id;
    }
  }
}
