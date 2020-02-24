package nl.knaw.huc.resussun.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.lifecycle.Managed;
import nl.knaw.huc.resussun.search.SearchClient;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

import java.io.IOException;

public class ManagedElasticSearchClient implements Managed, SearchClientFactory {
  private final RestHighLevelClient client;

  @JsonCreator
  public ManagedElasticSearchClient(
      @JsonProperty("hostName") String hostName,
      @JsonProperty("port") int port,
      @JsonProperty("scheme") String scheme) {
    client = new RestHighLevelClient(RestClient.builder(new HttpHost(hostName, port, scheme)));
  }

  public RestHighLevelClient getClient() {
    return client;
  }

  @Override
  public void start() {
  }

  @Override
  public void stop() throws IOException {
    client.close();
  }

  @Override
  public SearchClient createSearchClient() {
    return new SearchClient(getClient());
  }
}
