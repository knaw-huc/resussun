package nl.knaw.huc.resussun.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import nl.knaw.huc.resussun.configuration.UrlHelperFactory;
import nl.knaw.huc.resussun.timbuctoo.Timbuctoo;

public class ApiData {
  @JsonProperty
  private String dataSourceId;

  @JsonProperty
  private String timbuctooUrl;

  @JsonProperty
  private String timbuctooGuiUrl;

  @JsonCreator
  public ApiData(@JsonProperty("dataSourceId") String dataSourceId,
                 @JsonProperty("timbuctooUrl") String timbuctooUrl,
                 @JsonProperty("timbuctooGuiUrl") String timbuctooGuiUrl) {
    this.dataSourceId = dataSourceId;
    this.timbuctooUrl = timbuctooUrl;
    this.timbuctooGuiUrl = timbuctooGuiUrl;
  }

  public String getDataSourceId() {
    return dataSourceId;
  }

  public String getTimbuctooUrl() {
    return timbuctooUrl;
  }

  public String getTimbuctooGuiUrl() {
    return timbuctooGuiUrl;
  }

  @JsonIgnore
  public Timbuctoo getTimbuctoo() {
    return new Timbuctoo(timbuctooUrl);
  }

  @JsonIgnore
  public UrlHelperFactory.UrlHelper getTimbuctooGuiUrlHelper() {
    return new UrlHelperFactory(timbuctooGuiUrl).urlHelper();
  }
}
