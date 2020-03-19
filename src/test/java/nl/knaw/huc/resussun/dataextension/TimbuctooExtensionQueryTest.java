package nl.knaw.huc.resussun.dataextension;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.huc.resussun.model.DataExtensionResponse;
import nl.knaw.huc.resussun.model.DataExtensionResponse.LiteralPropertyValue;
import nl.knaw.huc.resussun.timbuctoo.TimbuctooRequest;
import nl.knaw.huc.resussun.timbuctoo.TimbuctooResponseMapper;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;

class TimbuctooExtensionQueryTest {
  @Test
  void createWorks() {
    String expectedQuery = "query extensionQuery {\n" +
        "  dataSets {\n" +
        "    u519bd710306620fa7c56d541ae7b9f5b7f57a706__clusius {\n" +
        "      http___timbuctoo_huygens_knaw_nl_datasets_clusius_Place_PL00000001: subject(uri: \"http://timbuctoo" +
        ".huygens.knaw.nl/datasets/clusius/Place_PL00000001\") {\n" +
        "        http___timbuctoo_huygens_knaw_nl_properties_name: getAllOfPredicate(uri: \"http://timbuctoo.huygens" +
        ".knaw.nl/properties/name\" outgoing:true) {\n" +
        "          values {\n" +
        "            value\n" +
        "          }\n" +
        "        }\n" +
        "        http___timbuctoo_huygens_knaw_nl_properties_country: getAllOfPredicate(uri: \"http://timbuctoo" +
        ".huygens.knaw.nl/properties/country\" " +
        "outgoing:true) {\n" +
        "          values {\n" +
        "            value\n" +
        "          }\n" +
        "        }\n" +
        "      }\n" +
        "      http___timbuctoo_huygens_knaw_nl_datasets_clusius_Place_PL00000003: subject(uri: \"http://timbuctoo" +
        ".huygens.knaw.nl/datasets/clusius/Place_PL00000003\") {\n" +
        "        http___timbuctoo_huygens_knaw_nl_properties_name: getAllOfPredicate(uri: \"http://timbuctoo.huygens" +
        ".knaw.nl/properties/name\" outgoing:true) {\n" +
        "          values {\n" +
        "            value\n" +
        "          }\n" +
        "        }\n" +
        "        http___timbuctoo_huygens_knaw_nl_properties_country: getAllOfPredicate(uri: \"http://timbuctoo" +
        ".huygens.knaw.nl/properties/country\" " +
        "outgoing:true) {\n" +
        "          values {\n" +
        "            value\n" +
        "          }\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "  }\n" +
        "}";
    List<String> subjects = Lists.newArrayList(
        "http://timbuctoo.huygens.knaw.nl/datasets/clusius/Place_PL00000001",
        "http://timbuctoo.huygens.knaw.nl/datasets/clusius/Place_PL00000003"
    );
    List<String> predicates = Lists.newArrayList(
        "http://timbuctoo.huygens.knaw.nl/properties/name",
        "http://timbuctoo.huygens.knaw.nl/properties/country"
    );
    String dataSetId = "u519bd710306620fa7c56d541ae7b9f5b7f57a706__clusius";
    TimbuctooExtensionQuery instance = new TimbuctooExtensionQuery(dataSetId, subjects, predicates);

    TimbuctooRequest actualRequest = instance.createQuery();

    assertThat(actualRequest.getQuery(), is(equalToIgnoringWhiteSpace(expectedQuery)));
    assertThat(actualRequest.getVariables().size(), is(0));
  }

  @Test
  void mapperWorks() throws Exception {
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
    String dataSetId = "u519bd710306620fa7c56d541ae7b9f5b7f57a706__clusius";
    String id = "http___timbuctoo_huygens_knaw_nl_datasets_clusius_Place_PL00000001";
    List<String> subjects = Lists.newArrayList(id);
    String property = "http___timbuctoo_huygens_knaw_nl_properties_country";
    List<String> predicates = Lists.newArrayList(property);
    TimbuctooResponseMapper<Map<String, Map<String, List<? extends DataExtensionResponse.PropertyValue>>>> mapper =
        new TimbuctooExtensionQuery(dataSetId, subjects, predicates).createMapper();

    Map<String, Map<String, List<? extends DataExtensionResponse.PropertyValue>>> values =
        mapper.mapResponse(new ObjectMapper().readTree(timResponse));

    assertThat(values, hasKey(id));
    assertThat(values.get(id), hasKey(property));
    assertThat(values.get(id).get(property), contains(new LiteralPropertyValue("Surinam")));
  }
}
