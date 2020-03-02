package nl.knaw.huc.resussun.tasks;

import io.dropwizard.servlets.tasks.Task;
import io.lettuce.core.api.StatefulRedisConnection;
import nl.knaw.huc.resussun.api.ApiClient;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public class DeleteIndexTask extends Task {
  private static final String DATA_SET_ID = "dataSetId";

  private final RestHighLevelClient elasticSearchClient;
  private final StatefulRedisConnection<String, String> redisConnection;

  public DeleteIndexTask(RestHighLevelClient elasticSearchClient,
                         StatefulRedisConnection<String, String> redisConnection) {
    super("deleteIndex");
    this.elasticSearchClient = elasticSearchClient;
    this.redisConnection = redisConnection;
  }

  @Override
  public void execute(Map<String, List<String>> params, PrintWriter out) throws Exception {
    if (!params.containsKey(DATA_SET_ID)) {
      out.println("Expected parameter: " + DATA_SET_ID);
      return;
    }

    String dataSetId = params.get(DATA_SET_ID).get(0);
    ApiClient apiClient = new ApiClient(redisConnection, dataSetId);

    if (!apiClient.hasApi()) {
      out.println("There is no index for the dataset with id " + dataSetId);
      return;
    }

    apiClient.deleteApiData();
    elasticSearchClient.indices().delete(new DeleteIndexRequest(dataSetId), RequestOptions.DEFAULT);
  }
}
