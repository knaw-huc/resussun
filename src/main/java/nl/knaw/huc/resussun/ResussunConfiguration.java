package nl.knaw.huc.resussun;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import nl.knaw.huc.resussun.configuration.ManagedElasticSearchClient;
import nl.knaw.huc.resussun.configuration.UrlHelperFactory;

import javax.validation.constraints.NotNull;

public class ResussunConfiguration extends Configuration {
  @NotNull
  @JsonProperty("elasticsearchClient")
  private ManagedElasticSearchClient managedElasticSearchClient;

  @NotNull
  @JsonProperty("urlHelper")
  private UrlHelperFactory urlHelperFactory;

  public ManagedElasticSearchClient getManagedElasticSearchClient() {
    return managedElasticSearchClient;
  }

  public UrlHelperFactory getUrlHelperFactory() {
    return urlHelperFactory;
  }
}
