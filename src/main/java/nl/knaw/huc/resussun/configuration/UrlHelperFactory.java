package nl.knaw.huc.resussun.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.ws.rs.core.UriBuilder;

public class UrlHelperFactory {
  private final String publicUrl;

  @JsonCreator
  public UrlHelperFactory(@JsonProperty("publicUrl") String publicUrl) {

    this.publicUrl = publicUrl;
  }

  public UrlHelper urlHelper() {
    return new UrlHelper(publicUrl);
  }


  public static class UrlHelper {

    private final UriBuilder uriBuilder;

    private UrlHelper(String publicUrl) {
      uriBuilder = UriBuilder.fromUri(publicUrl);
    }

    public UrlHelper path(String path) {
      uriBuilder.path(path);
      return this;
    }

    public UrlHelper queryParamTemplate(String name, String value) {
      uriBuilder.queryParam(name, value);
      return this;
    }

    public String template() {
      return uriBuilder.toTemplate();
    }
  }
}
