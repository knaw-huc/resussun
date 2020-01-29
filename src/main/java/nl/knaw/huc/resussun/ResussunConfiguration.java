package nl.knaw.huc.resussun;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import nl.knaw.huc.resussun.configuration.ElasticSearchClientFactory;

import javax.validation.constraints.NotNull;

public class ResussunConfiguration extends Configuration {
  @NotNull
  @JsonProperty("elasticsearchClient")
  private ElasticSearchClientFactory elasticSearchClientFactory;

  public ElasticSearchClientFactory getElasticSearchClientFactory() {
    return elasticSearchClientFactory;
  }

}
