package nl.knaw.huc.resussun.timbuctoo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class CollectionsMetadataMapper implements TimbuctooResponseMapper<Map<String, CollectionMetadata>> {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String COLLECTION_ID_PROP = "collectionId";
  private static final String COLLECTION_LIST_ID_PROP = "collectionListId";
  private static final String URI_PROP = "uri";
  private final String key;

  private CollectionsMetadataMapper(String key) {
    this.key = key;
  }

  private static CollectionMetadata mapCollectionMetadata(JsonNode collection) {
    String collectionId = collection.get(COLLECTION_ID_PROP).asText();
    String collectionListId = collection.get(COLLECTION_LIST_ID_PROP).asText();
    String uri = collection.get(URI_PROP).asText();

    try {
      List<PropertyMetadata> properties = Arrays.asList(
          MAPPER.treeToValue(collection.get("properties").get("items"), PropertyMetadata[].class)
      );
      return new CollectionMetadata(collectionId, collectionListId, uri, properties);
    } catch (JsonProcessingException e) {
      return new CollectionMetadata(collectionId, collectionListId, uri, Collections.emptyList());
    }
  }

  public static CollectionsMetadataMapper collectionIdAsKey() {
    return new CollectionsMetadataMapper(COLLECTION_ID_PROP);
  }

  public static TimbuctooRequest createCollectionsMetadataRequest(String datasetId) {
    return new TimbuctooRequest("query dataSetMetaData($dataSet:ID!) {\n" +
        "  dataSetMetadata(dataSetId: $dataSet) {\n" +
        "    collectionList {\n" +
        "      items {\n" +
        "        collectionId\n" +
        "        collectionListId\n" +
        "        properties {\n" +
        "          items {\n" +
        "            name\n" +
        "            isList\n" +
        "            isValueType\n" +
        "          }\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "  }\n" +
        "}", Map.of("dataSet", datasetId));
  }

  @Override
  public Map<String, CollectionMetadata> mapResponse(JsonNode json) {
    Iterable<JsonNode> collections = () -> json.get("data")
                                               .get("dataSetMetadata")
                                               .get("collectionList")
                                               .get("items")
                                               .iterator();

    return StreamSupport.stream(collections.spliterator(), false)
                        // FIXME make sure we do not exclude valid collections with Provenance and unknown in the id.
                        .filter(col -> !col.get(COLLECTION_ID_PROP).asText().contains("unknown"))
                        .filter(col -> !col.get(COLLECTION_ID_PROP).asText().contains("Provenance"))
                        .collect(Collectors.toMap(
                            col -> col.get(key).asText(),
                            CollectionsMetadataMapper::mapCollectionMetadata
                        ));
  }
}
