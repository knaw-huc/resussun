package nl.knaw.huc.resussun.tasks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import io.dropwizard.servlets.tasks.Task;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class CreateIndexTask extends Task {
  private static final String COLLECTION_LIST_QUERY = "query dataSetMetaData($dataSet:ID! ) {\n" +
      "  dataSetMetadata(dataSetId: $dataSet) {\n" +
      "    collectionList {\n" +
      "      items {\n" +
      "        collectionListId\n" +
      "      }\n" +
      "    }\n" +
      "  }\n" +
      "}";
  private static final String DATA_QUERY = "query data($cursor: ID) {\n" +
      "  dataSets {\n" +
      "    %s {\n" +
      "        %s(cursor: $cursor count: 1000) {\n" +
      "        nextCursor\n" +
      "        items {\n" +
      "          uri\t\n" +
      "          title {\n" +
      "            value\n" +
      "          }\n" +
      "          rdf_type {\n" +
      "            title {\n" +
      "              value\n" +
      "            }\n" +
      "          }\n" +
      "        }\n" +
      "      }\n" +
      "    }\n" +
      "  }\n" +
      "}";
  private static final String DATA_SET_ID = "dataSetId";
  private static final String TIMBUCTOO_URL = "timbuctooUrl";
  private static final List<String> PARAMS = Lists.newArrayList(DATA_SET_ID, TIMBUCTOO_URL);
  private static final Logger LOG = LoggerFactory.getLogger(CreateIndexTask.class);
  private final CloseableHttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final RestHighLevelClient elasticsearchClient;

  public CreateIndexTask(RestHighLevelClient elasticsearchClient){
    super("createIndex");
    httpClient = HttpClients.createDefault();
    objectMapper = new ObjectMapper();
    this.elasticsearchClient = elasticsearchClient;
  }

  @Override
  public void execute(Map<String, List<String>> params, PrintWriter out) throws Exception {
    if (!params.keySet().containsAll(PARAMS)) {
      out.println("Expected parameters: " + TIMBUCTOO_URL + " and " + DATA_SET_ID);
      return;
    }
    out.println(params);

    final String dataSetId = params.get(DATA_SET_ID).get(0);
    final String graphQlUrl = params.get(TIMBUCTOO_URL).get(0) + "/v5/graphql";
    final Stream<String> collectionListIds = getCollectionListIds(dataSetId, graphQlUrl, out);

    final List<String> collectionIdSet = collectionListIds.collect(Collectors.toList());
    out.println("Number of collections: " + collectionIdSet.size());
    for (String collectionId : collectionIdSet) {
      queryData(dataSetId, graphQlUrl, collectionId, out, null);
    }
    elasticsearchClient.close();
  }

  private void queryData(String dataSetId, String graphQlUrl, String collectionId, PrintWriter out, String cursor)
      throws IOException {
    out.println("queryData cursor: " + cursor + " collectionId: " + collectionId);
    out.flush();
    final ObjectNode dataQuery = objectMapper.createObjectNode();
    dataQuery.put("query", String.format(DATA_QUERY, dataSetId, collectionId));
    final JsonNode cursorVariable = objectMapper.createObjectNode().put("cursor", cursor);
    dataQuery.set("variables", cursorVariable);

    final StringEntity stringEntity = new StringEntity(objectMapper.writeValueAsString(dataQuery));
    stringEntity.setContentType("application/json");

    final HttpPost httpPost = new HttpPost(graphQlUrl);
    httpPost.setEntity(stringEntity);
    try (final CloseableHttpResponse response = httpClient.execute(httpPost)) {
      final HttpEntity entity = response.getEntity();

      if (response.getStatusLine().getStatusCode() != 200) {
        out.println("response status: " + response.getStatusLine().getStatusCode());
        out.println("response: " + EntityUtils.toString(entity));
        out.flush();
      } else {
        final JsonNode jsonNode = objectMapper.readTree(response.getEntity().getContent());

        processData(
            (ArrayNode) jsonNode.get("data").get("dataSets").get(dataSetId).get(collectionId).get("items"),
            out
        );

        final boolean hasNextCursor = !jsonNode.get("data")
                                               .get("dataSets")
                                               .get(dataSetId)
                                               .get(collectionId)
                                               .get("nextCursor").isNull();

        out.println("hasNextCursor: " + hasNextCursor);
        out.flush();

        if (hasNextCursor) {
          final String nextCursor = jsonNode.get("data")
                                            .get("dataSets")
                                            .get(dataSetId)
                                            .get(collectionId)
                                            .get("nextCursor").asText();

          queryData(dataSetId, graphQlUrl, collectionId, out, nextCursor);
        }
      }
    }
  }

  private void processData(ArrayNode data, PrintWriter out) {
    final BulkRequest bulkRequest = new BulkRequest();
    data.iterator().forEachRemaining(entity -> {
      try {
        bulkRequest.add(new IndexRequest("index")
            .id(entity.get("uri").asText())
            .source(objectMapper.writeValueAsString(entity), XContentType.JSON)
        );
      } catch (JsonProcessingException e) {
        LOG.error("Could not add field to request", e);
        out.println("Could not add field to request: " + e.getMessage());
        out.flush();
      }
    });

    try {
      elasticsearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);
    } catch (IOException e) {
      out.println("Could not process bulkRequest: " + e.getMessage());
      LOG.error("Could not process bulkRequest", e);
    }

  }

  private Stream<String> getCollectionListIds(String dataSetId, String graphQlUri, PrintWriter out) throws IOException {
    final ObjectNode metadataQuery = objectMapper.createObjectNode();
    metadataQuery.put("query", COLLECTION_LIST_QUERY);

    final JsonNode dataSetNode = objectMapper.createObjectNode().put("dataSet", dataSetId);
    metadataQuery.set("variables", dataSetNode);

    final StringEntity stringEntity = new StringEntity(objectMapper.writeValueAsString(metadataQuery));
    stringEntity.setContentType("application/json");

    final HttpPost postRequest = new HttpPost(graphQlUri);
    postRequest.setEntity(stringEntity);
    try (final CloseableHttpResponse response = httpClient.execute(postRequest)) {
      final HttpEntity entity = response.getEntity();

      if (response.getStatusLine().getStatusCode() != 200) {
        out.println("response status: " + response.getStatusLine().getStatusCode());
        out.println("response: " + EntityUtils.toString(entity));
        out.flush();
        LOG.warn("Could not process Timbuctoo query: {}", EntityUtils.toString(entity));
        return Stream.empty();
      }

      JsonNode jsonNode = objectMapper.readTree(entity.getContent());
      final Iterable<JsonNode> collections = () ->
          jsonNode.get("data").get("dataSetMetadata").get("collectionList").get("items").iterator();
      return StreamSupport.stream(collections.spliterator(), false)
                          .map(col -> col.get("collectionListId").asText())
                          .filter(col -> !col.contains("unknown"))
                          .filter(col -> !col.contains("Provenance"));
    }
  }
}
