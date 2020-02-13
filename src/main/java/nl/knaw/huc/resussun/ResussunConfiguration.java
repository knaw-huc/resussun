package nl.knaw.huc.resussun;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import nl.knaw.huc.resussun.configuration.ElasticSearchClientFactory;
import nl.knaw.huc.resussun.configuration.UrlHelperFactory;

import javax.validation.constraints.NotNull;

public class ResussunConfiguration extends Configuration {
  @NotNull
  @JsonProperty("elasticsearchClient")
  private ElasticSearchClientFactory elasticSearchClientFactory;

  @NotNull
  @JsonProperty("urlHelper")
  private UrlHelperFactory urlHelperFactory;

  public ElasticSearchClientFactory getElasticSearchClientFactory() {
    return elasticSearchClientFactory;
  }

  public UrlHelperFactory getUrlHelperFactory() {
    return urlHelperFactory;
  }
}
