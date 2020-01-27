package nl.knaw.huc.resussun.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class CreateIndexCommand extends Command {

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
  private final CloseableHttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final RestHighLevelClient elasticsearchClient;

  public CreateIndexCommand() {
    super("createIndex", "Creates an Elasticsearch index based on a Timbuctoo data set");
    httpClient = HttpClients.createDefault();
    objectMapper = new ObjectMapper();
    elasticsearchClient = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));
  }

  @Override
  public void configure(Subparser subparser) {
    subparser.addArgument("--tim")
             .dest("timbuctooUrl")
             .type(String.class)
             .required(true)
             .help("Url of the Timbuctoo instance to get the data from");

    subparser.addArgument("--dataSet")
             .dest("dataSetId")
             .type(String.class)
             .required(true)
             .help("The data set to index");
  }

  @Override
  public void run(Bootstrap<?> bootstrap, Namespace namespace) throws Exception {
    System.out.println(namespace.getAttrs());

    final String dataSetId = namespace.getString("dataSetId");
    final String graphQlUrl = namespace.getString("timbuctooUrl") + "/v5/graphql";
    final Stream<String> collectionListIds = getCollectionListIds(dataSetId, graphQlUrl);

    final List<String> collectionIdSet = collectionListIds.collect(Collectors.toList());
    System.out.println("Number of collections: " + collectionIdSet.size());
    for (String collectionId : collectionIdSet) {
      queryData(dataSetId, graphQlUrl, collectionId, null);
    }
    elasticsearchClient.close();
  }

  private void queryData(String dataSetId, String graphQlUrl, String collectionId, String cursor) throws IOException {
    System.out.println("queryData cursor: " + cursor + " collectionId: " + collectionId);
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
        System.out.println("response status: " + response.getStatusLine().getStatusCode());
        System.out.println("response: " + EntityUtils.toString(entity));
      } else {
        final JsonNode jsonNode = objectMapper.readTree(response.getEntity().getContent());

        processData((ArrayNode) jsonNode.get("data").get("dataSets").get(dataSetId).get(collectionId).get("items"));

        final boolean hasNextCursor = !jsonNode.get("data")
                                               .get("dataSets")
                                               .get(dataSetId)
                                               .get(collectionId)
                                               .get("nextCursor").isNull();

        System.out.println("hasNextCursor: " + hasNextCursor);

        if (hasNextCursor) {
          final String nextCursor = jsonNode.get("data")
                                            .get("dataSets")
                                            .get(dataSetId)
                                            .get(collectionId)
                                            .get("nextCursor").asText();

          queryData(dataSetId, graphQlUrl, collectionId, nextCursor);
        }
      }
    }
  }

  private void processData(ArrayNode data) {
    final BulkRequest bulkRequest = new BulkRequest();
    data.iterator().forEachRemaining(entity -> {
      try {
        bulkRequest.add(new IndexRequest("index")
            .id(entity.get("uri").asText())
            .source(objectMapper.writeValueAsString(entity), XContentType.JSON)
        );
      } catch (JsonProcessingException e) {
        System.err.println("could add field to request: " + e.getMessage());
      }
    });

    try {
      elasticsearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);
    } catch (IOException e) {
      System.err.println("Could not process bulkRequest: " + e.getMessage());
    }

  }

  private Stream<String> getCollectionListIds(String dataSetId, String graphQlUri) throws IOException {
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
        System.out.println("response status: " + response.getStatusLine().getStatusCode());
        System.out.println("response: " + EntityUtils.toString(entity));
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
