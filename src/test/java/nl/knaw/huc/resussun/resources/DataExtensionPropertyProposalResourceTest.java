package nl.knaw.huc.resussun.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import nl.knaw.huc.resussun.ApiDataMock;
import nl.knaw.huc.resussun.configuration.JsonWithPaddingInterceptor;
import nl.knaw.huc.resussun.configuration.UrlHelperFactory;
import nl.knaw.huc.resussun.search.SearchClient;
import nl.knaw.huc.resussun.timbuctoo.CollectionMetadata;
import nl.knaw.huc.resussun.timbuctoo.PropertyMetadata;
import nl.knaw.huc.resussun.timbuctoo.Timbuctoo;
import org.assertj.core.util.Sets;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.io.Resources.getResource;
import static nl.knaw.huc.resussun.resources.ApiParamConverterProviderMock.API_DATA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@ExtendWith(DropwizardExtensionsSupport.class)
class DataExtensionPropertyProposalResourceTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final Timbuctoo TIMBUCTOO = mock(Timbuctoo.class);
  private static final String ROOT_PATH = "/" + API_DATA.getDataSourceId();
  private static final SearchClient SEARCH_CLIENT = mock(SearchClient.class);
  private static final UrlHelperFactory URL_HELPER_FACTORY = new UrlHelperFactory("http://www.example.org");
  private static final ResourceExtension RESOURCES = ResourceExtension
      .builder()
      .addResource(new ApiResource(SEARCH_CLIENT, URL_HELPER_FACTORY))
      .addResource(new JsonWithPaddingInterceptor())
      .addResource(new ApiParamConverterProviderMock(ApiDataMock.fromApiData(API_DATA, TIMBUCTOO)))
      .build();
  private static final String REQUESTED_TYPE = "http://example.org/requestedType";

  @AfterEach
  public void tearDown() {
    Mockito.reset(TIMBUCTOO);
  }

  @Test
  void propertyProposalIsValid() throws Exception {
    final JsonSchema schema = createSchemaValidator("data_extension_property_proposal_schema.json");
    Mockito.when(TIMBUCTOO.executeRequest(any(), any())).thenReturn(createCollectionMetadata());
    final JsonNode response = RESOURCES.target(ROOT_PATH)
                                       .path("extend")
                                       .path("properties")
                                       .queryParam("type", REQUESTED_TYPE)
                                       .request()
                                       .get(JsonNode.class);
    final Set<ValidationMessage> validationReport = schema.validate(response);
    final String validationMessage = String.join("\n", validationReport.stream().map(ValidationMessage::getMessage)
                                                                       .collect(Collectors.toSet()));
    assertThat(validationMessage, validationReport, Matchers.hasSize(0));
  }

  private Map<String, CollectionMetadata> createCollectionMetadata() throws JsonProcessingException {

    final List<PropertyMetadata> propertyMetadataList = OBJECT_MAPPER.readValue("[\n" +
            "  {\n" +
            "    \"uri\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\",\n" +
            "    \"name\": \"rdf_type\",\n" +
            "    \"isList\": false,\n" +
            "    \"isValueType\": false\n" +
            "  },\n" +
            "  {\n" +
            "    \"uri\": \"http://timbuctoo.huygens.knaw.nl/properties/beginDate\",\n" +
            "    \"name\": \"tim_beginDate\",\n" +
            "    \"isList\": false,\n" +
            "    \"isValueType\": true\n" +
            "  },\n" +
            "  {\n" +
            "    \"uri\": \"http://timbuctoo.huygens.knaw.nl/properties/hasLocation\",\n" +
            "    \"name\": \"tim_hasLocation\",\n" +
            "    \"isList\": false,\n" +
            "    \"isValueType\": false\n" +
            "  },\n" +
            "  {\n" +
            "    \"uri\": \"http://timbuctoo.huygens.knaw.nl/properties/endDate\",\n" +
            "    \"name\": \"tim_endDate\",\n" +
            "    \"isList\": false,\n" +
            "    \"isValueType\": true\n" +
            "  },\n" +
            "  {\n" +
            "    \"uri\": \"http://timbuctoo.huygens.knaw.nl/properties/original_id\",\n" +
            "    \"name\": \"tim_original_id\",\n" +
            "    \"isList\": false,\n" +
            "    \"isValueType\": true\n" +
            "  },\n" +
            "  {\n" +
            "    \"uri\": \"http://timbuctoo.huygens.knaw.nl/properties/hasResident\",\n" +
            "    \"name\": \"tim_hasResident\",\n" +
            "    \"isList\": false,\n" +
            "    \"isValueType\": false\n" +
            "  }\n" +
            "]",
        new TypeReference<>() {
        });
    final Map<String, CollectionMetadata> dataSetMetadata = Maps.newHashMap();
    CollectionMetadata collectionMetadata = new CollectionMetadata(
        REQUESTED_TYPE,
        REQUESTED_TYPE + "List",
        "http://example.org/" + REQUESTED_TYPE,
        propertyMetadataList
    );
    dataSetMetadata.put(REQUESTED_TYPE, collectionMetadata);

    return dataSetMetadata;
  }

  @Test
  void propertyProposalReturnsOnlyValueProperties() throws Exception {
    Mockito.when(TIMBUCTOO.executeRequest(any(), any())).thenReturn(createCollectionMetadata());

    final JsonNode response = RESOURCES.target(ROOT_PATH)
                                       .path("extend")
                                       .path("properties")
                                       .queryParam("type", REQUESTED_TYPE)
                                       .request()
                                       .get(JsonNode.class);
    Set<JsonNode> properties = Sets.newHashSet();
    response.get("properties").forEach(properties::add);
    Set<String> propertyIds = properties.stream().map(prop -> prop.get("id").asText()).collect(Collectors.toSet());

    assertThat(propertyIds, containsInAnyOrder(
        "http://timbuctoo.huygens.knaw.nl/properties/beginDate",
        "http://timbuctoo.huygens.knaw.nl/properties/endDate",
        "http://timbuctoo.huygens.knaw.nl/properties/original_id"
    ));
  }

  private JsonSchema createSchemaValidator(String schemaPath) throws IOException {
    final JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance();
    return schemaFactory.getSchema(getResource(schemaPath).openStream());
  }

}
