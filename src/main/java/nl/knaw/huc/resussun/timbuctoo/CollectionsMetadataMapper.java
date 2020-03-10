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

public class CollectionsMetadataMapper implements TimbuctooResponseMapper<Map<String, List<PropertyMetadata>>> {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final String key;

  private CollectionsMetadataMapper(String key) {
    this.key = key;
  }

  private static List<PropertyMetadata> mapListOfMetadata(JsonNode collection) {
    try {
      return Arrays.asList(
          MAPPER.treeToValue(collection.get("properties").get("items"), PropertyMetadata[].class)
      );
    } catch (JsonProcessingException e) {
      return Collections.emptyList();
    }
  }

  public static CollectionsMetadataMapper collectionIdAsKey() {
    return new CollectionsMetadataMapper("collectionId");
  }
  public static TimbuctooRequest createCollectionsMetadataRequest(String datasetId) {
    return new TimbuctooRequest("query dataSetMetaData($dataSet:ID!) {\n" +
        "  dataSetMetadata(dataSetId: $dataSet) {\n" +
        "    collectionList {\n" +
        "      items {\n" +
        "        collectionId\n" +
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
  public Map<String, List<PropertyMetadata>> mapResponse(JsonNode json) {
    Iterable<JsonNode> collections = () -> json.get("data")
                                               .get("dataSetMetadata")
                                               .get("collectionList")
                                               .get("items")
                                               .iterator();

    return StreamSupport.stream(collections.spliterator(), false)
                        // FIXME make sure we do not exclude valid collections with Provenance and unknown in the id.
                        .filter(col -> !col.get("collectionId").asText().contains("unknown"))
                        .filter(col -> !col.get("collectionId").asText().contains("Provenance"))
                        .collect(Collectors.toMap(
                            col -> col.get(key).asText(),
                            CollectionsMetadataMapper::mapListOfMetadata
                        ));
  }
}
