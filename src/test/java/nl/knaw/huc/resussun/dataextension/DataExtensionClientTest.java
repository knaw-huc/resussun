package nl.knaw.huc.resussun.dataextension;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import nl.knaw.huc.resussun.api.ApiData;
import nl.knaw.huc.resussun.dataextension.DataExtensionRequest.Property;
import nl.knaw.huc.resussun.dataextension.DataExtensionResponse.PropertyValue;
import nl.knaw.huc.resussun.timbuctoo.Timbuctoo;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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

  public static final String DATA_SOURCE_ID = "dataSourceId";
  public static final String PROP_1 = "prop1";
  public static final String COLLECTION = "collection";
  public static final String ID1 = "http://example.org/1";
  public static final String ID2 = "http://example.org/2";
  private static final ApiData API_DATA = new ApiData(DATA_SOURCE_ID, "http://timbuctoo", "http://timbuctoo-gui");
  private DataExtensionClient instance;
  private Timbuctoo timbuctoo;

  @BeforeEach
  void setUp() {
    timbuctoo = mock(Timbuctoo.class);
    instance = new DataExtensionClient((url) -> timbuctoo);
  }

  @Test
  void returnsAValidResponseWithoutRowsForRequestsWithoutIds() throws Exception {
    final JsonSchema schema = createSchemaValidator("data_extension_response_schema.json");
    List<Property> properties = Lists.newArrayList();
    properties.add(new Property(PROP_1, null));
    DataExtensionRequest extensionRequest = new DataExtensionRequest(Lists.newArrayList(), properties);

    JsonNode extensionResponse = instance.createExtensionResponse(API_DATA, extensionRequest);

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
    properties.add(new Property(PROP_1, null));
    Map<String, Map<String, List<? extends PropertyValue>>> queryResponse = Maps.newHashMap();
    queryResponse.put(ID1, ImmutableMap.of(PROP_1,
        Lists.newArrayList(new DataExtensionResponse.LiteralPropertyValue("value1"))
    ));
    queryResponse.put(ID2, ImmutableMap.of(PROP_1,
        Lists.newArrayList(new DataExtensionResponse.LiteralPropertyValue("value2"))
    ));
    when(timbuctoo.executeRequest(any(), any())).thenReturn(queryResponse);
    final DataExtensionRequest extensionRequest = new DataExtensionRequest(Lists.newArrayList(ID1, ID2), properties);

    JsonNode extensionResponse = instance.createExtensionResponse(API_DATA, extensionRequest);

    final Set<ValidationMessage> validationReport = schema.validate(extensionResponse);
    final String validationMessage = String.join("\n", validationReport.stream().map(ValidationMessage::getMessage)
                                                                       .collect(Collectors.toList()));
    assertThat(validationMessage, validationReport, hasSize(0)); // check validity
    assertThat(extensionResponse.get("rows").has(ID1), is(true));
    assertThat(extensionResponse.get("rows").has(ID2), is(true));
  }

  private JsonSchema createSchemaValidator(String schemaPath) throws IOException {
    final JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance();
    return schemaFactory.getSchema(getResource(schemaPath).openStream());
  }
}
