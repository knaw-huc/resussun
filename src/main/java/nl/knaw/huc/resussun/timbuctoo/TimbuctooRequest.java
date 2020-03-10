package nl.knaw.huc.resussun.timbuctoo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TimbuctooRequest {
  @JsonProperty
  private final String query;
  @JsonProperty
  private final Map<String, String> variables;

  public TimbuctooRequest(String query, Map<String, String> variables) {
    this.query = query;
    this.variables = variables;
  }

}
