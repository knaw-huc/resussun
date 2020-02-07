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

public class CollectionsMetadataMapper implements TimbuctooResponseMapper<Map<String, List<CollectionMetadata>>> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public Map<String, List<CollectionMetadata>> mapResponse(JsonNode json) {
        Iterable<JsonNode> collections =
                () -> json.get("data").get("dataSetMetadata").get("collectionList").get("items").iterator();

        return StreamSupport.stream(collections.spliterator(), false)
                .filter(col -> !col.get("collectionId").asText().contains("unknown"))
                .filter(col -> !col.get("collectionId").asText().contains("Provenance"))
                .collect(Collectors.toMap(
                        col -> col.get("collectionId").asText(),
                        CollectionsMetadataMapper::mapListOfMetadata
                ));
    }

    private static List<CollectionMetadata> mapListOfMetadata(JsonNode collection) {
        try {
            return Arrays.asList(
                    MAPPER.treeToValue(collection.get("properties").get("items"), CollectionMetadata[].class));
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
