package nl.knaw.huc.resussun.timbuctoo;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Utils {
  public static List<String> getValues(JsonNode jsonNode) {
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
}
