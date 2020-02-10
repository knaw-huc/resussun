package nl.knaw.huc.resussun.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.huc.resussun.configuration.SearchClientFactory;
import nl.knaw.huc.resussun.model.Candidates;
import nl.knaw.huc.resussun.model.Query;
import nl.knaw.huc.resussun.model.ServiceManifest;
import nl.knaw.huc.resussun.search.SearchClient;
import org.glassfish.jersey.server.JSONP;
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
import java.util.HashMap;
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
  @JSONP(queryParam = "callback")
  public Response get(@QueryParam("queries") String queries) {
    return handleRequest(queries);
  }

  @POST
  @JSONP(queryParam = "callback")
  @Consumes("application/x-www-form-urlencoded")
  public Response post(@FormParam("queries") String queries) {
    return handleRequest(queries);
  }

  private Response handleRequest(String queriesJson) {
    if (queriesJson != null) {
      try {
        Map<String, Query> queries = objectMapper.readValue(queriesJson, new TypeReference<>() {
        });
        Map<String, Candidates> searchResults = search(queries);
        return Response.ok(searchResults).build();

      } catch (JsonProcessingException e) {
        LOG.info("request not supported: {}", e.getMessage());
        return Response.status(Response.Status.BAD_REQUEST).build();
      } catch (IOException e) {
        LOG.error("Could not execute query", e);
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
    final Map<String, Candidates> searchResult = new HashMap<>();

    try (final SearchClient searchClient = searchClientFactory.createSearchClient()) {
      for (Map.Entry<String, Query> querySet : queries.entrySet()) {
        final String field = querySet.getKey();
        final String queryText = querySet.getValue().getQuery();

        final Candidates results = new Candidates();
        searchResult.put(field, results);

        searchClient.search(queryText, results::addCandidate);
      }
    }

    return searchResult;
  }
}
