package nl.knaw.huc.resussun.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;

public class ElasticsearchHealthCheck extends HealthCheck {
  private final RestHighLevelClient client;

  public ElasticsearchHealthCheck(RestHighLevelClient client) {
    this.client = client;
  }

  @Override
  protected Result check() throws Exception {
    return client.ping(RequestOptions.DEFAULT) ?
        Result.builder().healthy().withMessage("Elasticsearch is available").build() :
        Result.unhealthy("Elasticsearch server is unavailable");
  }
}
