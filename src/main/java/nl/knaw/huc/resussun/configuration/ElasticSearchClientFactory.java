package nl.knaw.huc.resussun.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

public class ElasticSearchClientFactory {

  private final String hostName;
  private final int port;
  private final String scheme;

  @JsonCreator
  public ElasticSearchClientFactory(
    @JsonProperty("hostName") String hostName,
    @JsonProperty("port") int port,
    @JsonProperty("scheme") String scheme
  ) {
    this.hostName = hostName;
    this.port = port;
    this.scheme = scheme;
  }

  public RestHighLevelClient build() {
    return new RestHighLevelClient(RestClient.builder(new HttpHost(hostName, port, scheme)));
  }

}
