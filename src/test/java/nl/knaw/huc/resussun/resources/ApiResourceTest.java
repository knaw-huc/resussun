package nl.knaw.huc.resussun.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import nl.knaw.huc.resussun.configuration.JsonWithPaddingInterceptor;
import nl.knaw.huc.resussun.configuration.UrlHelperFactory;
import nl.knaw.huc.resussun.model.Candidate;
import nl.knaw.huc.resussun.model.Candidates;
import nl.knaw.huc.resussun.search.SearchClient;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.io.Resources.getResource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@ExtendWith(DropwizardExtensionsSupport.class)
public class ApiResourceTest {
  private static final String ROOT_PATH = "/" + ApiParamConverterProviderMock.API_DATA.getDataSourceId();
  private static final SearchClient SEARCH_CLIENT = mock(SearchClient.class);
  private static final UrlHelperFactory URL_HELPER_FACTORY = new UrlHelperFactory("http://www.example.org");
  private static final ResourceExtension RESOURCES = ResourceExtension
      .builder()
      .addResource(new ApiResource(SEARCH_CLIENT, URL_HELPER_FACTORY))
      .addResource(new JsonWithPaddingInterceptor())
      .addResource(new ApiParamConverterProviderMock())
      .build();

  @AfterEach
  public void tearDown() {
    Mockito.reset(SEARCH_CLIENT);
  }

  @Test
  public void serviceManifestIsValid() throws Exception {
    final JsonSchema schema = createSchemaValidator("service_manifest_schema.json");

    final JsonNode response = RESOURCES.target(ROOT_PATH).request().get(JsonNode.class);

    final Set<ValidationMessage> validationReport = schema.validate(response);
    final String validationMessage = String.join("\n", validationReport.stream().map(ValidationMessage::getMessage)
                                                                       .collect(Collectors.toSet()));
    assertThat(validationMessage, validationReport, Matchers.hasSize(0));
  }

  @Test
  public void serviceManifestCallbackIsValid() throws Exception {
    final String control = RESOURCES.target(ROOT_PATH).request().get(String.class);

    final String response = RESOURCES.target(ROOT_PATH).queryParam("callback", "callback").request().get(String.class);

    assertThat(response, is(String.format("callback(%s);", control)));
  }

  @Test
  public void emptyQueryResultIsValid() throws Exception {
    Mockito.doAnswer(invocation -> {
      return Map.of("q0", new Candidates(Collections.emptyList()));
    }).when(SEARCH_CLIENT).search(any(), any());
    final JsonSchema schema = createSchemaValidator("reconciliation_query_result_batch_schema.json");
    final Form form = new Form();
    form.param("queries", "{\"q0\":{\"query\":\"Amstelveen\",\"type\":\"\",\"type_strict\":\"should\"}}");

    final JsonNode response =
        RESOURCES.target(ROOT_PATH).request().buildPost(Entity.form(form)).submit(JsonNode.class).get();

    final Set<ValidationMessage> validationReport = schema.validate(response);
    final String validationMessage = String.join("\n", validationReport.stream().map(ValidationMessage::getMessage)
                                                                       .collect(Collectors.toSet()));
    assertThat(validationMessage, validationReport, Matchers.hasSize(0));
  }

  @Test
  public void nonEmptyQueryResultIsValid() throws Exception {
    Mockito.doAnswer(invocation -> {
      return Map.of("q0", new Candidates(List.of(
          new Candidate("id1", "name1", 100.0f, true),
          new Candidate("id2", "name2", 90.0f, true)
      )));
    }).when(SEARCH_CLIENT).search(any(), any());
    final JsonSchema schema = createSchemaValidator("reconciliation_query_result_batch_schema.json");
    final Form form = new Form();
    form.param("queries", "{\"q0\":{\"query\":\"Amstelveen\",\"type\":\"\",\"type_strict\":\"should\"}}");

    final JsonNode response =
        RESOURCES.target(ROOT_PATH).request().buildPost(Entity.form(form)).submit(JsonNode.class).get();

    final Set<ValidationMessage> validationReport = schema.validate(response);
    final String validationMessage = String.join("\n", validationReport.stream().map(ValidationMessage::getMessage)
                                                                       .collect(Collectors.toSet()));
    assertThat(validationMessage, validationReport, Matchers.hasSize(0));
  }

  private JsonSchema createSchemaValidator(String schemaPath) throws IOException {
    final JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance();
    return schemaFactory.getSchema(getResource(schemaPath).openStream());
  }
}
