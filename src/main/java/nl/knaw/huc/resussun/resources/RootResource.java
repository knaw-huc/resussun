package nl.knaw.huc.resussun.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.huc.resussun.configuration.SearchClientFactory;
import nl.knaw.huc.resussun.model.Candidates;
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
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Map;

@Path("/")
@Produces({"application/json", "application/javascript"})
public class RootResource {

  public static final Logger LOG = LoggerFactory.getLogger(RootResource.class);
  private final ObjectMapper objectMapper;
  private final SearchClientFactory searchClientFactory;

  public RootResource(SearchClientFactory searchClientFactory) {
    objectMapper = new ObjectMapper();
    this.searchClientFactory = searchClientFactory;
  }

  @GET
  public Response get(@QueryParam("queries") String queries, @QueryParam("callback") String callback) {
    return handleRequest(queries, callback);
  }

  @POST
  @Consumes("application/x-www-form-urlencoded")
  public Response post(@FormParam("queries") String queries, @QueryParam("callback") String callback) {
    return handleRequest(queries, callback);
  }

  private Response handleRequest(String queriesJson, String callback) {
    if (queriesJson != null) {
      try {
        Map<String, Query> queries = objectMapper.readValue(queriesJson, new TypeReference<>() {
        });
        return Response.ok(search(queries)).build();

      } catch (JsonProcessingException e) {
        LOG.info("request not supported: {}", e.getMessage());
        return Response.status(Response.Status.BAD_REQUEST).build();
      } catch (IOException e) {
        LOG.error("Could not execute query", e);
        return Response.serverError().build();
      }
    } else if (callback != null) {
      try {
        return Response.ok(String.format("%s(%s);", callback, objectMapper.writeValueAsString(createServiceManifest())))
                       .build();
      } catch (JsonProcessingException e) {
        LOG.error("Error processing manifest", e);
        return Response.serverError().build();
      }
    }
    return Response.ok(createServiceManifest()).build();

  }

  private ServiceManifest createServiceManifest() {
    return new ServiceManifest("Timbuctoo OpenRefine Recon API",
        "http://example.org/idetifierspace", "http://example.org/schemaspace");
  }

  private Map<String, Candidates> search(Map<String, Query> queries) throws IOException {
    try (final SearchClient searchClient = searchClientFactory.createSearchClient()) {
      return searchClient.search(queries);
    }
  }
}
