package nl.knaw.huc.resussun.dataextension;

import com.fasterxml.jackson.databind.JsonNode;
import nl.knaw.huc.resussun.model.DataExtensionResponse.LiteralPropertyValue;
import nl.knaw.huc.resussun.model.DataExtensionResponse.PropertyValue;
import nl.knaw.huc.resussun.model.DataExtensionResponse.ReferencePropertyValue;
import nl.knaw.huc.resussun.timbuctoo.PropertyMetadata;
import nl.knaw.huc.resussun.timbuctoo.TimbuctooRequest;
import nl.knaw.huc.resussun.timbuctoo.TimbuctooResponseMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class TimbuctooExtensionQuery {
  private final String dataSetId;
  private final List<String> subjects;
  private final List<PropertyMetadata> predicates;

  public TimbuctooExtensionQuery(String dataSetId, List<String> subjects, List<PropertyMetadata> predicates) {

    this.dataSetId = dataSetId;
    this.subjects = subjects;
    this.predicates = predicates;
  }

  public TimbuctooRequest createQuery() {
    String query = String.format("query extensionQuery {\n" +
            "  dataSets {\n" +
            "    %s {\n" +
            "      %s" +
            "    }\n" +
            "  }\n" +
            "}",
        dataSetId,
        createSubjectQuery(subjects, predicates)
    );

    return new TimbuctooRequest(query, new HashMap<>());
  }

  private String createSubjectQuery(List<String> subjects, List<PropertyMetadata> predicates) {
    StringBuilder subjectQuery = new StringBuilder();
    String predicateQuery = createPredicateQuery(predicates);
    for (String subject : subjects) {
      subjectQuery.append(String.format("%s: subject(uri: \"%s\") {\n" +
          "  %s" +
          "}\n", GraphQlHelper.escapeGraphQl(subject), subject, predicateQuery
      ));
    }

    return subjectQuery.toString();
  }

  private String createPredicateQuery(List<PropertyMetadata> predicates) {
    StringBuilder predicateQuery = new StringBuilder();
    for (PropertyMetadata predicate : predicates) {
      String predicateUri = predicate.getUri();
      predicateQuery.append(String.format("%s: getAllOfPredicate(uri: \"%s\" outgoing: %b) {\n" +
          createPredQueryBody(predicate.isValueType()) +
          "}\n", GraphQlHelper.escapeGraphQl(predicateUri), predicateUri, predicate.isOutgoing()
      ));
    }

    return predicateQuery.toString();
  }

  private String createPredQueryBody(boolean isValueType) {
    if (isValueType) {
      return "  values {\n" +
          "    value\n" +
          "  }\n";
    }

    return "  entities {\n" +
        "    uri\n" +
        "    title {\n" +
        "      value\n" +
        "    }\n" +
        "  }\n";
  }


  public TimbuctooResponseMapper<Map<String, Map<String, List<? extends PropertyValue>>>> createMapper() {
    return new TimbuctooExtensionQueryResponseMapper(dataSetId, subjects, predicates, GraphQlHelper::escapeGraphQl);
  }

  static class TimbuctooExtensionQueryResponseMapper
      implements TimbuctooResponseMapper<Map<String, Map<String, List<? extends PropertyValue>>>> {

    private final String dataSetId;
    private final List<String> subjects;
    private final List<PropertyMetadata> predicates;
    private final Function<String, String> escapeGraphQl;


    public TimbuctooExtensionQueryResponseMapper(
        String dataSetId,
        List<String> subjects,
        List<PropertyMetadata> predicates,
        Function<String, String> escapeGraphQl
    ) {
      this.dataSetId = dataSetId;
      this.subjects = subjects;
      this.predicates = predicates;
      this.escapeGraphQl = escapeGraphQl;
    }

    @Override
    public Map<String, Map<String, List<? extends PropertyValue>>> mapResponse(JsonNode timbuctooResponse) {
      JsonNode data = timbuctooResponse.get("data").get("dataSets").get(dataSetId);
      Map<String, Map<String, List<? extends PropertyValue>>> mappedResponse = new HashMap<>();

      for (String subject : subjects) {
        JsonNode subjectNode = data.get(escapeGraphQl.apply(subject));

        Map<String, List<? extends PropertyValue>> properties = new HashMap<>();

        for (PropertyMetadata predicate : predicates) {
          List<PropertyValue> values = new ArrayList();
          String predicateUri = predicate.getUri();
          if (predicate.isValueType()) {
            subjectNode.get(escapeGraphQl.apply(predicateUri)).get("values")
                       .forEach(val -> values.add(new LiteralPropertyValue(val.get("value").textValue())));
          } else {
            subjectNode.get(escapeGraphQl.apply(predicateUri)).get("entities")
                       .forEach(ref -> values.add(new ReferencePropertyValue(
                           ref.get("uri").asText(),
                           ref.get("title").get("value").asText() // title always has a value
                       )));
          }

          properties.put(predicate.getName(), values);
        }
        mappedResponse.put(subject, properties);

      }
      return mappedResponse;
    }
  }
}
