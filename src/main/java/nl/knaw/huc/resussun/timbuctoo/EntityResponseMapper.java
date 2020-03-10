package nl.knaw.huc.resussun.timbuctoo;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static nl.knaw.huc.resussun.timbuctoo.Utils.getValues;

public class EntityResponseMapper implements TimbuctooResponseMapper<EntityResponse> {
  public static TimbuctooRequest createEntityRequest(String datasetId,
                                                    String collectionId,
                                                    String uri,
                                                    List<String> properties) {
    return new TimbuctooRequest(String.format("query data($uri: String!) {\n" +
        "  dataSets {\n" +
        "    %s {\n" +
        "      %s(uri: $uri) {\n" +
        "        uri\n" +
        "        title { value }\n" +
        "        description { value }\n" +
        "        image { value }\n" +
        "        %s\n" +
        "      }\n" +
        "    }\n" +
        "  }\n" +
        "}", datasetId, collectionId, String.join("\n", properties)), Map.of("uri", uri));
  }

  @Override
  public EntityResponse mapResponse(JsonNode json) {
    json = json.get("data").get("dataSets");
    json = json.get(json.fieldNames().next());
    JsonNode root = json.get(json.fieldNames().next());

    String uri = root.get("uri").textValue();
    String title = getValues(root.get("title")).get(0);

    List<String> descriptionList = getValues(root.get("description"));
    String description = !descriptionList.isEmpty() ? descriptionList.get(0) : null;

    List<String> imageList = getValues(root.get("image"));
    String image = !imageList.isEmpty() ? imageList.get(0) : null;

    Map<String, List<String>> values = new HashMap<>();
    root.fields().forEachRemaining(field -> {
      String key = field.getKey();
      if (!List.of("uri", "title", "description", "image").contains(key)) {
        values.put(key, getValues(field.getValue()));
      }
    });

    return new EntityResponse(uri, title, description, image, values);
  }
}
