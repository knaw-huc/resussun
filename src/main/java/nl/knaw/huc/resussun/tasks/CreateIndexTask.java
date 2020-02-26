package nl.knaw.huc.resussun.tasks;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.dropwizard.servlets.tasks.Task;
import io.lettuce.core.api.StatefulRedisConnection;
import nl.knaw.huc.resussun.api.ApiClient;
import nl.knaw.huc.resussun.api.ApiData;
import nl.knaw.huc.resussun.timbuctoo.CollectionMetadata;
import nl.knaw.huc.resussun.timbuctoo.CollectionsMetadataMapper;
import nl.knaw.huc.resussun.timbuctoo.QueryResponse;
import nl.knaw.huc.resussun.timbuctoo.QueryResponseItem;
import nl.knaw.huc.resussun.timbuctoo.QueryResponseMapper;
import nl.knaw.huc.resussun.timbuctoo.Timbuctoo;
import nl.knaw.huc.resussun.timbuctoo.TimbuctooException;
import nl.knaw.huc.resussun.timbuctoo.TimbuctooRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CreateIndexTask extends Task {
  private static final String DATA_SET_ID = "dataSetId";
  private static final String TIMBUCTOO_URL = "timbuctooUrl";
  private static final List<String> PARAMS = List.of(DATA_SET_ID, TIMBUCTOO_URL);

  private final RestHighLevelClient elasticSearchClient;
  private final StatefulRedisConnection<String, String> redisConnection;

  public CreateIndexTask(RestHighLevelClient elasticSearchClient,
                         StatefulRedisConnection<String, String> redisConnection) {
    super("createIndex");
    this.elasticSearchClient = elasticSearchClient;
    this.redisConnection = redisConnection;
  }

  @Override
  public void execute(Map<String, List<String>> params, PrintWriter out) throws Exception {
    if (!params.keySet().containsAll(PARAMS)) {
      throw new Exception("Expected parameters: " + TIMBUCTOO_URL + " and " + DATA_SET_ID);
    }

    String dataSetId = params.get(DATA_SET_ID).get(0);
    ApiClient apiClient = new ApiClient(redisConnection, dataSetId);

    if (apiClient.hasApi()) {
      throw new Exception("There is already an index for the dataset with id " + dataSetId);
    }

    String timbuctooUrl = params.get(TIMBUCTOO_URL).get(0);
    Timbuctoo timbuctoo = new Timbuctoo(timbuctooUrl);

    createApi(apiClient, dataSetId, timbuctooUrl);
    createIndex(dataSetId);

    Map<String, List<CollectionMetadata>> collectionsMetadata = getCollectionsMetadata(timbuctoo, dataSetId);
    for (Map.Entry<String, List<CollectionMetadata>> collectionMetadata : collectionsMetadata.entrySet()) {
      List<String> props = getPropsFromMetadata(collectionMetadata.getValue());
      queryData(timbuctoo, dataSetId, collectionMetadata.getKey(), props, null);
    }
  }

  private void createApi(ApiClient apiClient, String datasetId, String timbuctooUrl) throws JsonProcessingException {
    ApiData apiData = new ApiData(datasetId, timbuctooUrl);
    apiClient.setApiData(apiData);
  }

  private void createIndex(String indexName) throws IOException {
    elasticSearchClient.indices().create(new CreateIndexRequest(indexName).mapping(
        "{\n" +
            "  \"properties\": {\n" +
            "    \"uri\": {\n" +
            "      \"type\": \"keyword\"\n" +
            "    },\n" +
            "    \"types\": {\n" +
            "      \"type\": \"keyword\"\n" +
            "    },\n" +
            "    \"title\": {\n" +
            "      \"type\": \"text\"\n" +
            "    },\n" +
            "    \"values\": {\n" +
            "      \"type\": \"text\"\n" +
            "    }\n" +
            "  }\n" +
            "}", XContentType.JSON), RequestOptions.DEFAULT);
  }

  private void queryData(Timbuctoo timbuctoo, String dataSetId, String collectionId, List<String> props, String cursor)
      throws TimbuctooException, IOException {
    TimbuctooRequest request = TimbuctooRequest.createQueryRequest(dataSetId, collectionId, props, cursor);
    QueryResponse queryResponse = timbuctoo.executeRequest(request, new QueryResponseMapper());

    processData(dataSetId, queryResponse.getItems());

    if (queryResponse.getNextCursor() != null) {
      queryData(timbuctoo, dataSetId, collectionId, props, queryResponse.getNextCursor());
    }
  }

  private void processData(String indexName, List<QueryResponseItem> data) throws IOException {
    BulkRequest bulkRequest = new BulkRequest();
    data.iterator().forEachRemaining(entity -> {
      IndexRequest indexRequest = new IndexRequest(indexName)
          .id(entity.getUri())
          .source(
              "uri", entity.getUri(),
              "types", entity.getTypes(),
              "title", entity.getTitle(),
              "values", entity.getValues().values().stream()
                              .flatMap(List::stream)
                              .collect(Collectors.joining(" "))
          );

      bulkRequest.add(indexRequest);
    });
    elasticSearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);
  }

  private static Map<String, List<CollectionMetadata>> getCollectionsMetadata(Timbuctoo timbuctoo, String dataSetId)
      throws TimbuctooException {
    TimbuctooRequest request = TimbuctooRequest.createCollectionsMetadataRequest(dataSetId);
    return timbuctoo.executeRequest(request, new CollectionsMetadataMapper());
  }

  private static List<String> getPropsFromMetadata(List<CollectionMetadata> collectionMetadata) {
    return collectionMetadata
        .stream()
        .filter(prop -> prop.getName().startsWith("rdf_type") || prop.isValueType())
        .map(prop -> {
          String valueExpr =
              prop.isValueType() ? "... on Value { value }" : "... on Entity { uri }";
          if (prop.isList()) {
            valueExpr = " items { " + valueExpr + " }";
          }
          return prop.getName() + " { " + valueExpr + " }";
        })
        .collect(Collectors.toList());
  }
}
