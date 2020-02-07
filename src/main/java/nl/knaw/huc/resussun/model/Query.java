package nl.knaw.huc.resussun.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Query {
  @JsonProperty
  private String query;

  @JsonProperty
  private String type;

  @JsonProperty
  private Integer limit;

  @JsonProperty("type_strict")
  private String typeStrict;

  public String getQuery() {
    return query;
  }

  public String getType() {
    return type;
  }

  public Integer getLimit() {
    return limit;
  }

  public String getTypeStrict() {
    return typeStrict;
  }
}
