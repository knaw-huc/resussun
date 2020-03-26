package nl.knaw.huc.resussun.dataextension;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.huc.resussun.model.DataExtensionResponse;
import nl.knaw.huc.resussun.model.DataExtensionResponse.LiteralPropertyValue;
import nl.knaw.huc.resussun.model.DataExtensionResponse.ReferencePropertyValue;
import nl.knaw.huc.resussun.timbuctoo.PropertyMetadata;
import nl.knaw.huc.resussun.timbuctoo.TimbuctooRequest;
import nl.knaw.huc.resussun.timbuctoo.TimbuctooResponseMapper;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;

class TimbuctooExtensionQueryTest {

  private static final String DATA_SET_ID = "u519bd710306620fa7c56d541ae7b9f5b7f57a706__clusius";

  @Test
  void createWorksWithLiteralProperties() {
    String expectedQuery = "query extensionQuery {\n" +
        "  dataSets {\n" +
        "    u519bd710306620fa7c56d541ae7b9f5b7f57a706__clusius {\n" +
        "      http___timbuctoo_huygens_knaw_nl_datasets_clusius_Place_PL00000001: subject(uri: \"http://timbuctoo" +
        ".huygens.knaw.nl/datasets/clusius/Place_PL00000001\") {\n" +
        "        http___timbuctoo_huygens_knaw_nl_properties_name: getAllOfPredicate(uri: \"http://timbuctoo.huygens" +
        ".knaw.nl/properties/name\" outgoing: true) {\n" +
        "          values {\n" +
        "            value\n" +
        "          }\n" +
        "        }\n" +
        "        http___timbuctoo_huygens_knaw_nl_properties_country: getAllOfPredicate(uri: \"http://timbuctoo" +
        ".huygens.knaw.nl/properties/country\" " +
        "outgoing: true) {\n" +
        "          values {\n" +
        "            value\n" +
        "          }\n" +
        "        }\n" +
        "      }\n" +
        "      http___timbuctoo_huygens_knaw_nl_datasets_clusius_Place_PL00000003: subject(uri: \"http://timbuctoo" +
        ".huygens.knaw.nl/datasets/clusius/Place_PL00000003\") {\n" +
        "        http___timbuctoo_huygens_knaw_nl_properties_name: getAllOfPredicate(uri: \"http://timbuctoo.huygens" +
        ".knaw.nl/properties/name\" outgoing: true) {\n" +
        "          values {\n" +
        "            value\n" +
        "          }\n" +
        "        }\n" +
        "        http___timbuctoo_huygens_knaw_nl_properties_country: getAllOfPredicate(uri: \"http://timbuctoo" +
        ".huygens.knaw.nl/properties/country\" " +
        "outgoing: true) {\n" +
        "          values {\n" +
        "            value\n" +
        "          }\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "  }\n" +
        "}";
    List<String> subjects = List.of(
        "http://timbuctoo.huygens.knaw.nl/datasets/clusius/Place_PL00000001",
        "http://timbuctoo.huygens.knaw.nl/datasets/clusius/Place_PL00000003"
    );
    Set<PropertyMetadata> predicates = new LinkedHashSet<>();
    predicates.add(
        new PropertyMetadata("http://timbuctoo.huygens.knaw.nl/properties/name", "tim_name", false, true, false)
    );
    predicates.add(
        new PropertyMetadata("http://timbuctoo.huygens.knaw.nl/properties/country", "tim_country", false, true, false)
    );
    TimbuctooExtensionQuery instance = new TimbuctooExtensionQuery(DATA_SET_ID, subjects, predicates);

    TimbuctooRequest actualRequest = instance.createQuery();

    assertThat(actualRequest.getQuery(), is(equalToIgnoringWhiteSpace(expectedQuery)));
    assertThat(actualRequest.getVariables().size(), is(0));
  }

  @Test
  void createWorksWithReferenceProperties() {
    String expectedQuery = "query extensionQuery {\n" +
        "  dataSets {\n" +
        "    u519bd710306620fa7c56d541ae7b9f5b7f57a706__clusius {\n" +
        "      http___timbuctoo_huygens_knaw_nl_datasets_clusius_Persons_PE00011941: subject(uri: \"http://timbuctoo" +
        ".huygens.knaw.nl/datasets/clusius/Persons_PE00011941\") {\n" +
        "        http___timbuctoo_huygens_knaw_nl_properties_hasBirthPlace: getAllOfPredicate(uri: \"http://timbuctoo" +
        ".huygens.knaw.nl/properties/hasBirthPlace\" outgoing: true) {\n" +
        "          entities {\n" +
        "            uri\n" +
        "            title {\n" +
        "              value\n" +
        "            }\n" +
        "          }\n" +
        "        }\n" +
        "        http___timbuctoo_huygens_knaw_nl_properties_hasMember: getAllOfPredicate(uri: \"http://timbuctoo" +
        ".huygens.knaw.nl/properties/hasMember\" outgoing: false) {\n" +
        "          entities {\n" +
        "            uri\n" +
        "            title {\n" +
        "              value\n" +
        "            }\n" +
        "          }\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "  }\n" +
        "}";


    List<String> subjects = List.of("http://timbuctoo.huygens.knaw.nl/datasets/clusius/Persons_PE00011941");
    Set<PropertyMetadata> predicates = new LinkedHashSet<>();
    predicates.add(
        new PropertyMetadata("http://timbuctoo.huygens.knaw.nl/properties/hasBirthPlace", "tim_hasBirthPlace", false,
            false, false)
    );
    predicates.add(
        new PropertyMetadata("http://timbuctoo.huygens.knaw.nl/properties/hasMember", "tim_hasMember", false, false,
            true)
    );
    TimbuctooExtensionQuery instance = new TimbuctooExtensionQuery(DATA_SET_ID, subjects, predicates);

    TimbuctooRequest actualRequest = instance.createQuery();

    assertThat(actualRequest.getQuery(), is(equalToIgnoringWhiteSpace(expectedQuery)));
    assertThat(actualRequest.getVariables().size(), is(0));
  }

  @Test
  void mapperWorksWithLiteralProperties() throws Exception {
    String timResponse = "{\n" +
        "  \"data\": {\n" +
        "    \"dataSets\": {\n" +
        "      \"u519bd710306620fa7c56d541ae7b9f5b7f57a706__clusius\": {\n" +
        "        \"http___timbuctoo_huygens_knaw_nl_datasets_clusius_Place_PL00000001\": {\n" +
        "          \"http___timbuctoo_huygens_knaw_nl_properties_country\": {\n" +
        "            \"values\": [\n" +
        "              {\n" +
        "                \"value\": \"Surinam\"\n" +
        "              }\n" +
        "            ]\n" +
        "          }\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "  }\n" +
        "}";
    String id = "http___timbuctoo_huygens_knaw_nl_datasets_clusius_Place_PL00000001";
    List<String> subjects = Lists.newArrayList(id);
    String property = "tim_country";
    Set<PropertyMetadata> predicates = Set.of(
        new PropertyMetadata("http://timbuctoo.huygens.knaw.nl/properties/country", property, false, true, false)
    );
    TimbuctooResponseMapper<Map<String, Map<String, List<? extends DataExtensionResponse.PropertyValue>>>> mapper =
        new TimbuctooExtensionQuery(DATA_SET_ID, subjects, predicates).createMapper();

    Map<String, Map<String, List<? extends DataExtensionResponse.PropertyValue>>> values =
        mapper.mapResponse(new ObjectMapper().readTree(timResponse));

    assertThat(values, hasKey(id));
    assertThat(values.get(id), hasKey(property));
    assertThat(values.get(id).get(property), contains(new LiteralPropertyValue("Surinam")));
  }

  @Test
  void mapperWorksWithReferenceProperties() throws Exception {
    String timResponse = "{\n" +
        "  \"data\": {\n" +
        "    \"dataSets\": {\n" +
        "      \"u519bd710306620fa7c56d541ae7b9f5b7f57a706__clusius\": {\n" +
        "        \"http___timbuctoo_huygens_knaw_nl_datasets_clusius_Persons_PE00011941\": {\n" +
        "          \"http___timbuctoo_huygens_knaw_nl_properties_hasBirthPlace\": {\n" +
        "            \"entities\": [\n" +
        "              {\n" +
        "                \"uri\": \"http://timbuctoo.huygens.knaw.nl/datasets/clusius/Place_PL00000063\",\n" +
        "                \"title\": {\n" +
        "                  \"value\": \"place\"\n" +
        "                }\n" +
        "              }\n" +
        "            ]\n" +
        "          },\n" +
        "          \"http___timbuctoo_huygens_knaw_nl_properties_hasMember\": {\n" +
        "            \"entities\": [\n" +
        "              {\n" +
        "                \"uri\": \"http://timbuctoo.huygens.knaw.nl/datasets/clusius/Membership_ME00006362\",\n" +
        "                \"title\": {\n" +
        "                  \"value\": \"membership\"\n" +
        "                }\n" +
        "              }\n" +
        "            ]\n" +
        "          }\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "  }\n" +
        "}";
    String id = "http://timbuctoo.huygens.knaw.nl/datasets/clusius/Persons_PE00011941";
    List<String> subjects = List.of(id);
    String prop1 = "tim_hasBirthPlace";
    String prop2 = "tim_hasMember";
    Set<PropertyMetadata> predicates = Set.of(
        new PropertyMetadata("http://timbuctoo.huygens.knaw.nl/properties/hasBirthPlace", prop1, false, false, false),
        new PropertyMetadata("http://timbuctoo.huygens.knaw.nl/properties/hasMember", prop2, false, false, true)
    );

    TimbuctooResponseMapper<Map<String, Map<String, List<? extends DataExtensionResponse.PropertyValue>>>> mapper =
        new TimbuctooExtensionQuery(DATA_SET_ID, subjects, predicates).createMapper();

    Map<String, Map<String, List<? extends DataExtensionResponse.PropertyValue>>> values =
        mapper.mapResponse(new ObjectMapper().readTree(timResponse));

    assertThat(values, hasKey(id));
    assertThat(values.get(id), hasKey(prop1));
    assertThat(values.get(id).get(prop1), contains(new ReferencePropertyValue("http://timbuctoo.huygens.knaw.nl/datasets/clusius/Place_PL00000063", "place")));
    assertThat(values.get(id), hasKey(prop2));
    assertThat(values.get(id).get(prop2), contains(new ReferencePropertyValue("http://timbuctoo.huygens.knaw.nl/datasets/clusius/Membership_ME00006362", "membership")));
  }
}
