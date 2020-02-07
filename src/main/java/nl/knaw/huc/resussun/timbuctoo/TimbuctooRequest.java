package nl.knaw.huc.resussun.timbuctoo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TimbuctooRequest {
  @JsonProperty
  private final String query;
  @JsonProperty
  private final Map<String, String> variables;

  public TimbuctooRequest(String query, Map<String, String> variables) {
    this.query = query;
    this.variables = variables;
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

  public static TimbuctooRequest createQueryRequest(String datasetId,
                                                    String collectionId,
                                                    List<String> properties,
                                                    String cursor
  ) {
    return new TimbuctooRequest(String.format("query data($cursor: ID) {\n" +
        "  dataSets {\n" +
        "    %s {\n" +
        "      %sList(cursor: $cursor count: 1000) {\n" +
        "        nextCursor\n" +
        "        items {\n" +
        "          uri\n" +
        "          title { value }\n" +
        "          %s\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "  }\n" +
        "}", datasetId, collectionId, String.join("\n", properties)),
        cursor != null ? Map.of("cursor", cursor) : Collections.emptyMap());
  }
}
