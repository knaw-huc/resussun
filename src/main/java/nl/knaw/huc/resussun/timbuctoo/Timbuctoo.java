package nl.knaw.huc.resussun.timbuctoo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static java.net.http.HttpRequest.BodyPublishers.ofByteArray;

public class Timbuctoo {
  private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();
  private final String url;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

      int attempt = 1;
      HttpResponse<byte[]> response = null;
      while (attempt <= 10 && (response == null || response.statusCode() != 200)) {
        response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        attempt++;
      }

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
    } catch (InterruptedException | IOException e) {
      throw new TimbuctooException("Failed to execute a request to Timbuctoo", e);
    }
  }
}
