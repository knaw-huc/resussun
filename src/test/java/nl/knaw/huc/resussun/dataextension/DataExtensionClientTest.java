package nl.knaw.huc.resussun.dataextension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import nl.knaw.huc.resussun.ApiDataMock;
import nl.knaw.huc.resussun.api.ApiData;
import nl.knaw.huc.resussun.dataextension.TimbuctooExtensionQuery.TimbuctooExtensionQueryResponseMapper;
import nl.knaw.huc.resussun.model.DataExtensionRequest;
import nl.knaw.huc.resussun.model.DataExtensionRequest.Property;
import nl.knaw.huc.resussun.model.DataExtensionResponse;
import nl.knaw.huc.resussun.model.DataExtensionResponse.PropertyValue;
import nl.knaw.huc.resussun.timbuctoo.CollectionMetadata;
import nl.knaw.huc.resussun.timbuctoo.CollectionsMetadataMapper;
import nl.knaw.huc.resussun.timbuctoo.PropertyMetadata;
import nl.knaw.huc.resussun.timbuctoo.Timbuctoo;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.io.Resources.getResource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DataExtensionClientTest {

  private static final String DATA_SOURCE_ID = "dataSourceId";
  private static final String VALUE_PROP_ID = "tim_beginDate";
  private static final String VALUE_PROP_NAME = "http://timbuctoo.huygens.knaw.nl/properties/beginDate";
  private static final String ID1 = "http://example.org/1";
  private static final String ID2 = "http://example.org/2";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String REF_PROP_NAME = "http://timbuctoo.huygens.knaw.nl/properties/hasLocation";
  private static final String REF_PROP_ID = "tim_hasLocation";
  private static final String REF_COL_ID = "clusius_Places";
  private static final String REF_COL_NAME = "http://example.org/place";
  private ApiData apiData;
  private DataExtensionClient instance;
  private Timbuctoo timbuctoo;

  @BeforeEach
  void setUp() {
    timbuctoo = mock(Timbuctoo.class);
    apiData = new ApiDataMock(DATA_SOURCE_ID, "http://timbuctoo", "http://timbuctoo-gui", timbuctoo);
    instance = new DataExtensionClient();
  }

  @Test
  void returnsAValidResponseWithoutRowsForRequestsWithoutIds() throws Exception {
    final JsonSchema schema = createSchemaValidator("data_extension_response_schema.json");
    List<Property> properties = Lists.newArrayList();
    properties.add(new Property(VALUE_PROP_ID, null));
    DataExtensionRequest extensionRequest = new DataExtensionRequest(Lists.newArrayList(), properties);
    when(timbuctoo.executeRequest(any(), any(CollectionsMetadataMapper.class))).thenReturn(createCollectionMetadata());

    JsonNode extensionResponse = instance.createExtensionResponse(apiData, extensionRequest);

    final Set<ValidationMessage> validationReport = schema.validate(extensionResponse);
    final String validationMessage = String.join("\n", validationReport.stream().map(ValidationMessage::getMessage)
                                                                       .collect(Collectors.toList()));
    assertThat(validationMessage, validationReport, hasSize(0)); // check validity
    assertThat(extensionResponse.get("rows").isEmpty(), is(true)); // check if rows is empty
  }

  @Test
  void returnsAValidResponseForRequestsWithIds() throws Exception {
    final JsonSchema schema = createSchemaValidator("data_extension_response_schema.json");
    List<Property> properties = Lists.newArrayList();
    properties.add(new Property(VALUE_PROP_ID, null));
    Map<String, Map<String, List<? extends PropertyValue>>> queryResponse = Maps.newHashMap();
    queryResponse.put(ID1, ImmutableMap.of(VALUE_PROP_ID,
        Lists.newArrayList(new DataExtensionResponse.LiteralPropertyValue("value1"))
    ));
    queryResponse.put(ID2, ImmutableMap.of(VALUE_PROP_ID,
        Lists.newArrayList(new DataExtensionResponse.LiteralPropertyValue("value2"))
    ));
    when(timbuctoo.executeRequest(any(), any(TimbuctooExtensionQueryResponseMapper.class))).thenReturn(queryResponse);
    when(timbuctoo.executeRequest(any(), any(CollectionsMetadataMapper.class))).thenReturn(createCollectionMetadata());
    final DataExtensionRequest extensionRequest = new DataExtensionRequest(Lists.newArrayList(ID1, ID2), properties);

    JsonNode extensionResponse = instance.createExtensionResponse(apiData, extensionRequest);

    final Set<ValidationMessage> validationReport = schema.validate(extensionResponse);
    final String validationMessage = String.join("\n", validationReport.stream().map(ValidationMessage::getMessage)
                                                                       .collect(Collectors.toList()));
    assertThat(validationMessage, validationReport, hasSize(0)); // check validity
    assertThat(extensionResponse.get("rows").has(ID1), is(true));
    assertThat(extensionResponse.get("rows").has(ID2), is(true));
  }

  @Test
  void returnsRightMetadataForLiteralProperties() throws Exception {
    List<Property> properties = Lists.newArrayList();
    properties.add(new Property(VALUE_PROP_ID, null));
    final DataExtensionRequest extensionRequest = new DataExtensionRequest(Lists.newArrayList(), properties);
    Map<String, Map<String, List<? extends PropertyValue>>> queryResponse = Maps.newHashMap();
    when(timbuctoo.executeRequest(any(), any(TimbuctooExtensionQueryResponseMapper.class))).thenReturn(queryResponse);
    when(timbuctoo.executeRequest(any(), any(CollectionsMetadataMapper.class))).thenReturn(createCollectionMetadata());

    JsonNode extensionResponse = instance.createExtensionResponse(apiData, extensionRequest);

    assertThat(extensionResponse.get("meta").get(0).get("name").asText(), is(VALUE_PROP_NAME));
    assertThat(extensionResponse.get("meta").get(0).get("id").asText(), is(VALUE_PROP_ID));

  }

  @Test
  void returnsMetaWithTypeInformationForRefProperties() throws Exception {
    List<Property> properties = Lists.newArrayList();
    properties.add(new Property(REF_PROP_ID, null));
    final DataExtensionRequest extensionRequest = new DataExtensionRequest(Lists.newArrayList(), properties);
    Map<String, Map<String, List<? extends PropertyValue>>> queryResponse = Maps.newHashMap();
    when(timbuctoo.executeRequest(any(), any(TimbuctooExtensionQueryResponseMapper.class))).thenReturn(queryResponse);
    when(timbuctoo.executeRequest(any(), any(CollectionsMetadataMapper.class))).thenReturn(createCollectionMetadata());
    final ObjectNode type = OBJECT_MAPPER.createObjectNode()
                                         .put("name", REF_COL_NAME)
                                         .put("id", REF_COL_NAME  );
    final ObjectNode expectedPropertyMetadata = OBJECT_MAPPER.createObjectNode();
    expectedPropertyMetadata.put("name", REF_PROP_NAME)
                            .put("id", REF_PROP_ID)
                            .set("type", type);

    JsonNode extensionResponse = instance.createExtensionResponse(apiData, extensionRequest);

    assertThat(extensionResponse.get("meta").get(0), is(expectedPropertyMetadata));
  }

  private Map<String, CollectionMetadata> createCollectionMetadata() throws JsonProcessingException {

    final List<PropertyMetadata> propertyMetadataList = OBJECT_MAPPER.readValue("[\n" +
            "  {\n" +
            "    \"uri\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\",\n" +
            "    \"name\": \"rdf_type\",\n" +
            "    \"isList\": false,\n" +
            "    \"isValueType\": false,\n" +
            "    \"isInverse\": false,\n" +
            "    \"referencedCollections\": {\n" +
            "      \"items\": [\n" +
            "        \"http___timbuctoo_huygens_knaw_nl_static_v5_vocabulary_unknown\"\n" +
            "      ]\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"uri\": \"http://timbuctoo.huygens.knaw.nl/properties/beginDate\",\n" +
            "    \"name\": \"tim_beginDate\",\n" +
            "    \"isList\": false,\n" +
            "    \"isValueType\": true,\n" +
            "    \"isInverse\": false,\n" +
            "    \"referencedCollections\": {\n" +
            "      \"items\": []\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"uri\": \"http://timbuctoo.huygens.knaw.nl/properties/hasLocation\",\n" +
            "    \"name\": \"tim_hasLocation\",\n" +
            "    \"isList\": false,\n" +
            "    \"isValueType\": false,\n" +
            "    \"isInverse\": false,\n" +
            "    \"referencedCollections\": {\n" +
            "      \"items\": [\n" +
            "        \"clusius_Places\"\n" +
            "      ]\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"uri\": \"http://timbuctoo.huygens.knaw.nl/properties/endDate\",\n" +
            "    \"name\": \"tim_endDate\",\n" +
            "    \"isList\": false,\n" +
            "    \"isValueType\": true,\n" +
            "    \"isInverse\": false,\n" +
            "    \"referencedCollections\": {\n" +
            "      \"items\": []\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"uri\": \"http://timbuctoo.huygens.knaw.nl/properties/original_id\",\n" +
            "    \"name\": \"tim_original_id\",\n" +
            "    \"isList\": false,\n" +
            "    \"isValueType\": true,\n" +
            "    \"isInverse\": false,\n" +
            "    \"referencedCollections\": {\n" +
            "      \"items\": []\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"uri\": \"http://timbuctoo.huygens.knaw.nl/properties/hasResident\",\n" +
            "    \"name\": \"tim_hasResident\",\n" +
            "    \"isList\": false,\n" +
            "    \"isValueType\": false,\n" +
            "    \"isInverse\": false,\n" +
            "    \"referencedCollections\": {\n" +
            "      \"items\": [\n" +
            "        \"clusius_Persons\"\n" +
            "      ]\n" +
            "    }\n" +
            "  }\n" +
            "]",
        new TypeReference<>() {
        });
    final Map<String, CollectionMetadata> dataSetMetadata = Maps.newHashMap();
    CollectionMetadata collectionMetadata = new CollectionMetadata(
        "REQUESTED_TYPE",
        "REQUESTED_TYPE" + "List",
        "http://example.org/" + "REQUESTED_TYPE",
        propertyMetadataList
    );
    dataSetMetadata.put("REQUESTED_TYPE", collectionMetadata);
    final CollectionMetadata referenceCollection = new CollectionMetadata(
        REF_COL_ID,
        REF_COL_ID + "List",
        REF_COL_NAME,
        new ArrayList<>()
    );
    dataSetMetadata.put(REF_COL_ID, referenceCollection);


    return dataSetMetadata;
  }

  private JsonSchema createSchemaValidator(String schemaPath) throws IOException {
    final JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance();
    return schemaFactory.getSchema(getResource(schemaPath).openStream());
  }

}
