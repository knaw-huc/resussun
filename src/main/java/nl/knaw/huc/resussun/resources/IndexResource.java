package nl.knaw.huc.resussun.resources;

import nl.knaw.huc.resussun.api.ApiClient;
import nl.knaw.huc.resussun.configuration.UrlHelperFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

@Path("/")
public class IndexResource {

  private final ApiClient apiClient;
  private final UrlHelperFactory urlHelperFactory;

  public IndexResource(ApiClient apiClient,
                       UrlHelperFactory urlHelperFactory) {

    this.apiClient = apiClient;
    this.urlHelperFactory = urlHelperFactory;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAvailableApis() {
    final List<String> keys = apiClient.getAllApiKeys()
                                       .map(key -> urlHelperFactory.urlHelper(key).template())
                                       .collect(Collectors.toList());
    return Response.ok(keys).build();
  }
}
