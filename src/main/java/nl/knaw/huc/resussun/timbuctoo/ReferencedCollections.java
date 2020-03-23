package nl.knaw.huc.resussun.timbuctoo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ReferencedCollections {
  private final List<String> items;

  @JsonCreator
  public ReferencedCollections(@JsonProperty("items") List<String> items) {
    this.items = items;
  }

  public List<String> getItems() {
    return items;
  }
}
