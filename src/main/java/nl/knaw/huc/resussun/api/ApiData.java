package nl.knaw.huc.resussun.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiData {
  @JsonProperty
  private String dataSourceId;

  @JsonProperty
  private String timbuctooUrl;

  @JsonCreator
  public ApiData(@JsonProperty("dataSourceId") String dataSourceId, @JsonProperty("timbuctooUrl") String timbuctooUrl) {
    this.dataSourceId = dataSourceId;
    this.timbuctooUrl = timbuctooUrl;
  }

  public String getDataSourceId() {
    return dataSourceId;
  }

  public String getTimbuctooUrl() {
    return timbuctooUrl;
  }
}
