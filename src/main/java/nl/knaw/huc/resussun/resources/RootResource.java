package nl.knaw.huc.resussun.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.huc.resussun.configuration.ElasticSearchClientFactory;
import nl.knaw.huc.resussun.model.Candidate;
import nl.knaw.huc.resussun.model.Candidates;
import nl.knaw.huc.resussun.model.Query;
import nl.knaw.huc.resussun.model.ServiceManifest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
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
import java.util.List;
import java.util.Map;

@Path("/")
@Produces({"application/json", "application/javascript"})
public class RootResource {

  public static final Logger LOG = LoggerFactory.getLogger(RootResource.class);
  private final ObjectMapper objectMapper;
  private final ElasticSearchClientFactory elasticSearchClientFactory;

  public RootResource(ElasticSearchClientFactory elasticSearchClientFactory) {
    objectMapper = new ObjectMapper();
    this.elasticSearchClientFactory = elasticSearchClientFactory;
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
    final Map<String, Candidates> candidates = new HashMap<>();

    try (final RestHighLevelClient elasticsearchClient = elasticSearchClientFactory.build()) {
      for (Map.Entry<String, Query> querySet : queries.entrySet()) {
        final String field = querySet.getKey();
        final String queryText = querySet.getValue().getQuery();

        final SearchSourceBuilder query =
                new SearchSourceBuilder().query(QueryBuilders.queryStringQuery("*" + queryText + "*").queryName(field));
        final SearchResponse response =
                elasticsearchClient.search(new SearchRequest("index").source(query), RequestOptions.DEFAULT);

        final Candidates results = new Candidates();
        candidates.put(field, results);

        for (final SearchHit hit : response.getHits()) {
          final Map<String, Object> source = hit.getSourceAsMap();
          final Candidate candidate = new Candidate(
                  hit.getId(),
                  source.get("title").toString(),
                  hit.getScore() * 100,
                  false
          );

          List<String> types = (List<String>) source.get("types");
          types.forEach(type -> candidate.type(type, type));

          results.candidate(candidate);
        }
      }
    }

    return candidates;
  }
}
