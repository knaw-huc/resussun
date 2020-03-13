package nl.knaw.huc.resussun.timbuctoo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;

public class TimbuctooRequest {
  @JsonProperty
  private final String query;
  @JsonProperty
  private final Map<String, String> variables;

  public TimbuctooRequest(String query, Map<String, String> variables) {
    this.query = query;
    this.variables = variables;
  }

  public String getQuery() {
    return query;
  }

  public Map<String, String> getVariables() {
    return variables;
  }
}
