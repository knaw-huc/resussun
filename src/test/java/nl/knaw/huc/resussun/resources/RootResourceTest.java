package nl.knaw.huc.resussun.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import nl.knaw.huc.resussun.configuration.ElasticSearchClientFactory;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.io.Resources.getResource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

@ExtendWith(DropwizardExtensionsSupport.class)
public class RootResourceTest {
  private static final ElasticSearchClientFactory ELASTICSEARCH_FACTORY = mock(ElasticSearchClientFactory.class);
  public static final ResourceExtension RESOURCES = ResourceExtension
      .builder()
      .addResource(new RootResource(ELASTICSEARCH_FACTORY))
      .build();

  @AfterEach
  public void tearDown() {
    reset(ELASTICSEARCH_FACTORY);
  }

  @Test
  public void serviceManifestIsValid() throws Exception {
    final JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance();
    final JsonSchema schema = schemaFactory.getSchema(
        getResource("service_manifest_schema.json").openStream()
    );

    final JsonNode response = RESOURCES.target("/").request().get(JsonNode.class);

    final Set<ValidationMessage> validationReport = schema.validate(response);

    final String validationMessage = String.join("\n", validationReport.stream().map(ValidationMessage::getMessage)
                                                                       .collect(Collectors.toSet()));
    assertThat(validationMessage, validationReport, Matchers.hasSize(0));
  }

}
