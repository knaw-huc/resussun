package nl.knaw.huc.resussun.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import nl.knaw.huc.resussun.configuration.SearchClientFactory;
import nl.knaw.huc.resussun.model.Candidate;
import nl.knaw.huc.resussun.search.SearchClient;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import java.io.IOException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.io.Resources.getResource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;


@ExtendWith(DropwizardExtensionsSupport.class)
public class RootResourceTest {
  private static final SearchClientFactory SEARCH_CLIENT_FACTORY = mock(SearchClientFactory.class);
  public static final ResourceExtension RESOURCES = ResourceExtension
      .builder()
      .addResource(new RootResource(SEARCH_CLIENT_FACTORY))
      .build();
  private SearchClient searchClient;


  @BeforeEach
  public void setUp() {
    searchClient = mock(SearchClient.class);
    when(SEARCH_CLIENT_FACTORY.createSearchClient()).thenReturn(searchClient);
  }

  @AfterEach
  public void tearDown() {
    reset(SEARCH_CLIENT_FACTORY);
  }

  @Test
  public void serviceManifestIsValid() throws Exception {
    final JsonSchema schema = createSchemaValidator("service_manifest_schema.json");

    final JsonNode response = RESOURCES.target("/").request().get(JsonNode.class);

    final Set<ValidationMessage> validationReport = schema.validate(response);
    final String validationMessage = String.join("\n", validationReport.stream().map(ValidationMessage::getMessage)
                                                                       .collect(Collectors.toSet()));
    assertThat(validationMessage, validationReport, Matchers.hasSize(0));
  }

  @Test
  public void emptyQueryResultIsValid() throws Exception {
    Mockito.doNothing().when(searchClient).search(any(), any());
    final JsonSchema schema = createSchemaValidator("reconciliation_query_result_batch_schema.json");
    final Form form = new Form();
    form.param("queries", "{\"q0\":{\"query\":\"Amstelveen\",\"type\":\"\",\"type_strict\":\"should\"}}");

    final JsonNode response = RESOURCES.target("/").request().buildPost(Entity.form(form)).submit(JsonNode.class).get();

    final Set<ValidationMessage> validationReport = schema.validate(response);
    final String validationMessage = String.join("\n", validationReport.stream().map(ValidationMessage::getMessage)
                                                                       .collect(Collectors.toSet()));
    assertThat(validationMessage, validationReport, Matchers.hasSize(0));
  }

  @Test
  public void nonEmptyQueryResultIsValid() throws Exception {
    Mockito.doAnswer(invocation -> {
      Consumer<Candidate> candidateConsumer = invocation.getArgument(1);
      candidateConsumer.accept(new Candidate("id1", "name1", 100.0f, true));
      candidateConsumer.accept(new Candidate("id2", "name2", 90.0f, true));
      return null;
    }).when(searchClient).search(any(), any());
    final JsonSchema schema = createSchemaValidator("reconciliation_query_result_batch_schema.json");
    final Form form = new Form();
    form.param("queries", "{\"q0\":{\"query\":\"Amstelveen\",\"type\":\"\",\"type_strict\":\"should\"}}");

    final JsonNode response = RESOURCES.target("/").request().buildPost(Entity.form(form)).submit(JsonNode.class).get();

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
