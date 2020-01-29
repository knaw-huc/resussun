package nl.knaw.huc.resussun.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import nl.knaw.huc.resussun.configuration.ElasticSearchClientFactory;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;

public class ElasticsearchHealthCheck extends HealthCheck {
  private final ElasticSearchClientFactory elasticsearchClientFactory;

  public ElasticsearchHealthCheck(ElasticSearchClientFactory elasticSearchClientFactory) {
    this.elasticsearchClientFactory = elasticSearchClientFactory;
  }

  @Override
  protected Result check() throws Exception {
    try(final RestHighLevelClient elasticsearchClient = elasticsearchClientFactory.build()) {
      boolean canReach = elasticsearchClient.ping(RequestOptions.DEFAULT);

      return canReach ?
          Result.builder().healthy().withMessage("Elasticsearch is available").build() :
          Result.unhealthy("Elasticsearch server is unavailable");
    }
  }
}
