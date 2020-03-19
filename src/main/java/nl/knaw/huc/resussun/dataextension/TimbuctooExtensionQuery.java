package nl.knaw.huc.resussun.dataextension;

import com.fasterxml.jackson.databind.JsonNode;
import nl.knaw.huc.resussun.model.DataExtensionResponse.LiteralPropertyValue;
import nl.knaw.huc.resussun.model.DataExtensionResponse.PropertyValue;
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
  private final List<String> predicates;

  public TimbuctooExtensionQuery(String dataSetId, List<String> subjects, List<String> predicates) {

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

  private String createSubjectQuery(List<String> subjects, List<String> predicates) {
    StringBuilder subjectQuery = new StringBuilder();
    String predicateQuery = createPredicateQuery(predicates);
    for (String subject : subjects) {
      subjectQuery.append(String.format("%s: subject(uri: \"%s\") {\n" +
          "  %s\n" +
          "}\n", escapeGraphQl(subject), subject, predicateQuery
      ));
    }

    return subjectQuery.toString();
  }

  private String createPredicateQuery(List<String> predicates) {
    StringBuilder predicateQuery = new StringBuilder();
    for (String predicate : predicates) {
      predicateQuery.append(String.format("%s: getAllOfPredicate(uri: \"%s\" outgoing:true) {\n" +
          "  values {\n" +
          "    value\n" +
          "  }\n" +
          "}\n", escapeGraphQl(predicate), predicate
      ));
    }

    return predicateQuery.toString();
  }

  private String escapeGraphQl(String label) {
    return label.replaceAll("[!$().:=@\\[\\]{|}/#]", "_");
  }


  public TimbuctooResponseMapper<Map<String, Map<String, List<? extends PropertyValue>>>> createMapper() {
    return new TimbuctooExtensionQueryResponseMapper(dataSetId, subjects, predicates, this::escapeGraphQl);
  }

  static class TimbuctooExtensionQueryResponseMapper
      implements TimbuctooResponseMapper<Map<String, Map<String, List<? extends PropertyValue>>>> {

    private final String dataSetId;
    private final List<String> subjects;
    private final List<String> predicates;
    private final Function<String, String> escapeGraphQl;


    public TimbuctooExtensionQueryResponseMapper(
        String dataSetId,
        List<String> subjects,
        List<String> predicates,
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
        for (String predicate : predicates) {
          List<PropertyValue> values = new ArrayList<>();
          subjectNode.get(escapeGraphQl.apply(predicate)).get("values")
                     .forEach(val -> values.add(new LiteralPropertyValue(val.get("value").textValue())));

          properties.put(predicate, values);
        }
        mappedResponse.put(subject, properties);

      }
      return mappedResponse;
    }
  }
}
