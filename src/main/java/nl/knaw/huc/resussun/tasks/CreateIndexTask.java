package nl.knaw.huc.resussun.tasks;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.dropwizard.servlets.tasks.Task;
import nl.knaw.huc.resussun.api.ApiClient;
import nl.knaw.huc.resussun.api.ApiData;
import nl.knaw.huc.resussun.timbuctoo.PropertyMetadata;
import nl.knaw.huc.resussun.timbuctoo.CollectionsMetadataMapper;
import nl.knaw.huc.resussun.timbuctoo.QueryResponse;
import nl.knaw.huc.resussun.timbuctoo.QueryResponseItem;
import nl.knaw.huc.resussun.timbuctoo.QueryResponseMapper;
import nl.knaw.huc.resussun.timbuctoo.Timbuctoo;
import nl.knaw.huc.resussun.timbuctoo.TimbuctooException;
import nl.knaw.huc.resussun.timbuctoo.TimbuctooRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.script.Script;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CreateIndexTask extends Task {
  private static final String DATA_SET_ID = "dataSetId";
  private static final String TIMBUCTOO_URL = "timbuctooUrl";
  private static final String TIMBUCTOO_GUI_URL = "timbuctooGuiUrl";
  private static final List<String> PARAMS = List.of(DATA_SET_ID, TIMBUCTOO_URL, TIMBUCTOO_GUI_URL);

  private final RestHighLevelClient elasticSearchClient;
  private final ApiClient apiClient;

  public CreateIndexTask(RestHighLevelClient elasticSearchClient, ApiClient apiClient) {
    super("createIndex");
    this.elasticSearchClient = elasticSearchClient;
    this.apiClient = apiClient;
  }

  @Override
  public void execute(Map<String, List<String>> params, PrintWriter out) throws Exception {
    if (!params.keySet().containsAll(PARAMS)) {
      out.println("Expected parameters: " + DATA_SET_ID + " and " + TIMBUCTOO_URL + " and " + TIMBUCTOO_GUI_URL);
      return;
    }

    String dataSetId = params.get(DATA_SET_ID).get(0);

    if (this.apiClient.hasApi(dataSetId)) {
      out.println("There is already an index for the dataset with id " + dataSetId);
      return;
    }

    String timbuctooUrl = params.get(TIMBUCTOO_URL).get(0);
    String timbuctooGuiUrl = params.get(TIMBUCTOO_GUI_URL).get(0);
    ApiData apiData = createApi(this.apiClient, dataSetId, timbuctooUrl, timbuctooGuiUrl);
    Timbuctoo timbuctoo = apiData.getTimbuctoo();

    createIndex(dataSetId);

    Map<String, List<PropertyMetadata>> collectionsMetadata = getCollectionsMetadata(timbuctoo, dataSetId);
    for (Map.Entry<String, List<PropertyMetadata>> collectionMetadata : collectionsMetadata.entrySet()) {
      List<String> props = getPropsFromMetadata(collectionMetadata.getValue());
      queryData(timbuctoo, dataSetId, collectionMetadata.getKey(), props, null);
    }
  }

  private ApiData createApi(ApiClient apiClient, String datasetId, String timbuctooUrl,
                            String timbuctooGuiUrl) throws JsonProcessingException {
    ApiData apiData = new ApiData(datasetId, timbuctooUrl, timbuctooGuiUrl);
    apiClient.setApiData(apiData, datasetId);
    return apiData;
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
            "    \"collectionIds\": {\n" +
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
    TimbuctooRequest request = QueryResponseMapper.createQueryRequest(dataSetId, collectionId, props, cursor);
    QueryResponse queryResponse = timbuctoo.executeRequest(request, new QueryResponseMapper());

    processData(dataSetId, collectionId, queryResponse.getItems());

    if (queryResponse.getNextCursor() != null) {
      queryData(timbuctoo, dataSetId, collectionId, props, queryResponse.getNextCursor());
    }
  }

  private void processData(String indexName, String collectionId, List<QueryResponseItem> data) throws IOException {
    BulkRequest bulkRequest = new BulkRequest();
    data.iterator().forEachRemaining(entity -> {
      UpdateRequest updateRequest = new UpdateRequest()
          .index(indexName)
          .id(entity.getUri())
          .script(new Script("ctx._source.collectionIds.add('" + collectionId + "')"))
          .upsert(
              "uri", entity.getUri(),
              "types", entity.getTypes(),
              "collectionIds", List.of(collectionId),
              "title", entity.getTitle(),
              "values", entity.getValues().values().stream()
                              .flatMap(List::stream)
                              .collect(Collectors.joining(" "))
          );

      bulkRequest.add(updateRequest);
    });
    elasticSearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);
  }

  private static Map<String, List<PropertyMetadata>> getCollectionsMetadata(Timbuctoo timbuctoo, String dataSetId)
      throws TimbuctooException {
    TimbuctooRequest request = CollectionsMetadataMapper.createCollectionsMetadataRequest(dataSetId);
    return timbuctoo.executeRequest(request, new CollectionsMetadataMapper());
  }

  private static List<String> getPropsFromMetadata(List<PropertyMetadata> propertyMetadata) {
    return propertyMetadata
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
