package nl.knaw.huc.resussun.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.MainResponse;

public class ElasticsearchHealthCheck extends HealthCheck {
  private final RestHighLevelClient elasticsearchClient;

  public ElasticsearchHealthCheck(RestHighLevelClient elasticsearchClient) {
    this.elasticsearchClient = elasticsearchClient;
  }

  @Override
  protected Result check() throws Exception {
    boolean canReach = elasticsearchClient.ping(RequestOptions.DEFAULT);

    return canReach ?
      Result.builder().healthy().withMessage("Elasticsearch is available").build() :
      Result.unhealthy("Elasticsearch server is unavailable");
  }
}
