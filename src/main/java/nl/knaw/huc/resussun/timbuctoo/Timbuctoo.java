package nl.knaw.huc.resussun.timbuctoo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static java.net.http.HttpRequest.BodyPublishers.ofByteArray;

public class Timbuctoo {
  private static final Logger LOG = LoggerFactory.getLogger(Timbuctoo.class);
  private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final String url;
  private int attempt = 1;

  public Timbuctoo(String url) {
    this.url = url;
  }

  public <T> T executeRequest(TimbuctooRequest timbuctooRequest, TimbuctooResponseMapper<T> responseMapper)
      throws TimbuctooException {
    try {
      HttpRequest request = HttpRequest.newBuilder()
                                       .uri(URI.create(url + "/v5/graphql"))
                                       .header("Content-Type", "application/json")
                                       .POST(ofByteArray(OBJECT_MAPPER.writeValueAsBytes(timbuctooRequest)))
                                       .build();

      HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());

      attempt = 1;
      if (response.statusCode() != 200) {
        throw new TimbuctooException("Request to Timbuctoo did not yield a successful status code");
      }

      JsonNode json = OBJECT_MAPPER.readTree(response.body());
      if (json.has("errors") && json.get("errors").isArray()) {
        throw new TimbuctooException(
            "Timbuctoo responded with error: " +
            json.get("errors").elements().next().toPrettyString()
        );
      }

      return responseMapper.mapResponse(json);
    } catch (IOException | InterruptedException e) {
      if (attempt > 10) {
        throw new TimbuctooException("Failed to execute a request to Timbuctoo", e);
      }
    } finally {
      try {
        attempt++;
        Thread.sleep(10000L * attempt);
      } catch (InterruptedException e) {
        LOG.debug("Interrupted!", e);
      }
    }

    return this.executeRequest(timbuctooRequest, responseMapper);
  }
}
