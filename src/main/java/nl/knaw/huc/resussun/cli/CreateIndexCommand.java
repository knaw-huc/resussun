package nl.knaw.huc.resussun.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Set;
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

  public CreateIndexCommand() {
    super("createIndex", "Creates an Elasticsearch index based on a Timbuctoo data set");
    httpClient = HttpClients.createDefault();
    objectMapper = new ObjectMapper();
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

    final Set<String> collectionIdSet = collectionListIds.collect(Collectors.toSet());
    for (String collectionId : collectionIdSet) {
      System.out.println("collectionId");
      queryData(dataSetId, graphQlUrl, collectionId, null);
    }
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

        final String nextCursor = jsonNode.get("data")
                                          .get("dataSets")
                                          .get(dataSetId)
                                          .get(collectionId)
                                          .get("nextCursor").asText();

        if (nextCursor != null && !nextCursor.equals("null")) {
          queryData(dataSetId, graphQlUrl, collectionId, nextCursor);
        }
      }
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
