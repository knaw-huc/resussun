package nl.knaw.huc.resussun.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.huc.resussun.api.ApiData;
import nl.knaw.huc.resussun.configuration.UrlHelperFactory;
import nl.knaw.huc.resussun.model.Preview;
import nl.knaw.huc.resussun.model.Query;
import nl.knaw.huc.resussun.model.ServiceManifest;
import nl.knaw.huc.resussun.search.SearchClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Map;

@Path("/{api}")
public class ApiResource {
  private static final Logger LOG = LoggerFactory.getLogger(ApiResource.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final SearchClient searchClient;
  private final UrlHelperFactory urlHelperFactory;

  public ApiResource(SearchClient searchClient, UrlHelperFactory urlHelperFactory) {
    this.searchClient = searchClient;
    this.urlHelperFactory = urlHelperFactory;
  }

  @GET
  @Produces({"application/json", "application/javascript"})
  public Response get(@PathParam("api") ApiData api, @QueryParam("queries") String queries) {
    return handleRequest(api, queries);
  }

  @POST
  @Consumes("application/x-www-form-urlencoded")
  @Produces({"application/json", "application/javascript"})
  public Response post(@PathParam("api") ApiData api, @FormParam("queries") String queries) {
    return handleRequest(api, queries);
  }

  @Path("/preview")
  public PreviewResource preview(@PathParam("api") ApiData api) {
    return new PreviewResource(searchClient, api);
  }

  private Response handleRequest(ApiData api, String queriesJson) {
    if (queriesJson != null) {
      try {
        Map<String, Query> queries = OBJECT_MAPPER.readValue(queriesJson, new TypeReference<>() {
        });
        return Response.ok(searchClient.search(api.getDataSourceId(), queries)).build();
      } catch (JsonProcessingException e) {
        LOG.info("request not supported: {}", e.getMessage());
        return Response.status(Response.Status.BAD_REQUEST).build();
      } catch (IOException e) {
        LOG.error("Could not execute query", e);
        return Response.serverError().build();
      }
    }

    return Response.ok(createServiceManifest(api)).build();
  }

  private ServiceManifest createServiceManifest(ApiData api) {
    final String previewUrl = urlHelperFactory.urlHelper(api.getDataSourceId()).path("preview")
                                              .queryParamTemplate("id", "{{id}}").template();

    return new ServiceManifest(
        String.format("Dataset \"%s\" of \"%s\" OpenRefine Recon API", api.getDataSourceId(), api.getTimbuctooUrl()),
        "http://example.org/idetifierspace", "http://example.org/schemaspace"
    ).preview(new Preview(previewUrl, 200, 300));
  }
}
