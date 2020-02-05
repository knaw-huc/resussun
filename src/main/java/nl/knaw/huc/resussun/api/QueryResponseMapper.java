package nl.knaw.huc.resussun.api;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class QueryResponseMapper implements TimbuctooResponseMapper<QueryResponse> {
    @Override
    public QueryResponse mapResponse(JsonNode json) {
        json = json.get("data").get("dataSets");
        json = json.get(json.fieldNames().next());
        JsonNode root = json.get(json.fieldNames().next());

        String cursor = !root.get("nextCursor").isNull() ? root.get("nextCursor").asText() : null;

        Iterable<JsonNode> itemsIterable = () -> root.get("items").iterator();
        List<Map<String, List<String>>> items = StreamSupport.stream(itemsIterable.spliterator(), false)
                .map(QueryResponseMapper::mapItem)
                .collect(Collectors.toList());

        return new QueryResponse(cursor, items);
    }

    private static Map<String, List<String>> mapItem(JsonNode item) {
        Map<String, List<String>> values = new HashMap<>();
        item.fields().forEachRemaining(field -> {
            if (field.getValue().isTextual())
                values.put(field.getKey(), List.of(field.getValue().textValue()));
            else if (field.getValue().has("value"))
                values.put(field.getKey(), List.of(field.getValue().get("value").textValue()));
            else if (field.getValue().has("uri"))
                values.put(field.getKey(), List.of(field.getValue().get("uri").textValue()));
            else if (field.getValue().has("items")) {
                Iterable<JsonNode> itemsIterable = () -> field.getValue().get("items").iterator();
                values.put(field.getKey(), StreamSupport.stream(itemsIterable.spliterator(), false)
                        .map(value -> value.has("value")
                                ? value.get("value").textValue() : value.get("uri").textValue())
                        .collect(Collectors.toList()));
            }
        });
        return values;
    }
}
