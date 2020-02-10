package nl.knaw.huc.resussun.timbuctoo;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class QueryResponseMapper implements TimbuctooResponseMapper<QueryResponse> {
  private static QueryResponseItem mapItem(JsonNode itemJson) {
    String uri = itemJson.get("uri").textValue();
    String title = getValues(itemJson.get("title")).get(0);
    List<String> types = itemJson.has("rdf_type") ?
        getValues(itemJson.get("rdf_type")) :
        getValues(itemJson.get("rdf_typeList"));

    Map<String, List<String>> values = new HashMap<>();
    itemJson.fields().forEachRemaining(field -> {
      String key = field.getKey();
      if (!List.of("uri", "title", "rdf_type", "rdf_typeList").contains(key)) {
        values.put(key, getValues(field.getValue()));
      }
    });

    return new QueryResponseItem(uri, title, types, values);
  }

  private static List<String> getValues(JsonNode jsonNode) {
    if (jsonNode.isTextual()) {
      return List.of(jsonNode.textValue());
    }

    if (jsonNode.has("value")) {
      return List.of(jsonNode.get("value").textValue());
    }

    if (jsonNode.has("uri")) {
      return List.of(jsonNode.get("uri").textValue());
    }

    if (jsonNode.has("items")) {
      List<String> itemValues = new ArrayList<>(jsonNode.get("items").findValuesAsText("value"));
      itemValues.addAll(jsonNode.get("items").findValuesAsText("uri"));
      return itemValues;
    }

    return Collections.emptyList();
  }

  @Override
  public QueryResponse mapResponse(JsonNode json) {
    json = json.get("data").get("dataSets");
    json = json.get(json.fieldNames().next());
    JsonNode root = json.get(json.fieldNames().next());

    String cursor = !root.get("nextCursor").isNull() ? root.get("nextCursor").asText() : null;

    Iterable<JsonNode> itemsIterable = () -> root.get("items").iterator();
    List<QueryResponseItem> items = StreamSupport.stream(itemsIterable.spliterator(), false)
                                                 .map(QueryResponseMapper::mapItem)
                                                 .collect(Collectors.toList());

    return new QueryResponse(cursor, items);
  }
}
