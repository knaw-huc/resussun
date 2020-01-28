package nl.knaw.huc.resussun.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

public class ElasticSearchClientFactory {
  private final RestHighLevelClient restHighLevelClient;

  @JsonCreator
  public ElasticSearchClientFactory(
    @JsonProperty("hostName") String hostName,
    @JsonProperty("port") int port,
    @JsonProperty("scheme") String scheme
  ) {
    restHighLevelClient = new RestHighLevelClient(RestClient.builder(new HttpHost(hostName, port, scheme)));
  }

  public RestHighLevelClient build() {
    return restHighLevelClient;
  }

}
