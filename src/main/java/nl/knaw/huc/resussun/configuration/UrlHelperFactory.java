package nl.knaw.huc.resussun.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class UrlHelperFactory {
  private final String publicUrl;

  @JsonCreator
  public UrlHelperFactory(@JsonProperty("publicUrl") String publicUrl) {
    this.publicUrl = publicUrl;
  }

  public UrlHelper urlHelper() {
    return urlHelper(null);
  }

  public UrlHelper urlHelper(String dataSourceId) {
    final UrlHelper urlHelper = new UrlHelper(publicUrl);
    if (dataSourceId != null) {
      urlHelper.path(dataSourceId);
    }
    return urlHelper;
  }

  public static class UrlHelper {
    private final UriBuilder uriBuilder;

    private UrlHelper(String publicUrl) {
      uriBuilder = UriBuilder.fromUri(publicUrl);
    }

    public UrlHelper path(String path) {
      uriBuilder.path(URLEncoder.encode(path, StandardCharsets.UTF_8));
      return this;
    }

    public UrlHelper queryParamTemplate(String name, String value) {
      uriBuilder.queryParam(name, value);
      return this;
    }

    public String template() {
      return uriBuilder.toTemplate();
    }

    public URI toUri() {
      return uriBuilder.build();
    }
  }
}
