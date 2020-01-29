package nl.knaw.huc.resussun.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import nl.knaw.huc.resussun.configuration.ElasticSearchClientFactory;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Iterator;

@Path("/")
public class RootResource {

  public static final Logger LOG = LoggerFactory.getLogger(RootResource.class);
  private final ObjectMapper objectMapper;
  private final ElasticSearchClientFactory elasticSearchClientFactory;

  public RootResource(ElasticSearchClientFactory elasticSearchClientFactory) {
    objectMapper = new ObjectMapper();
    this.elasticSearchClientFactory = elasticSearchClientFactory;
  }

  @GET
  @Produces("application/json")
  public Response get(ObjectNode request, @QueryParam("callback") String callback) {
    return handleRequest(request, callback);
  }

  private Response handleRequest(ObjectNode request, String callback) {
    final ObjectNode rootNode = objectMapper.createObjectNode();

    rootNode.put("name", "Timbuctoo OpenRefine Recon API");
    rootNode.put("identifierSpace", "http://example.org/idetifierspace");
    rootNode.put("schemaSpace", "http://example.org/schemaspace");

    if (callback != null) {
      return Response.ok(String.format("%s(%s);", callback, rootNode)).build();
    }
    return Response.ok(rootNode).build();
  }

  @POST
  @Consumes("application/x-www-form-urlencoded")
  @Produces("application/json")
  public Response query(MultivaluedMap<String, String> request) {
    LOG.info("request: {}", request);

    try {
      final JsonNode queries;
      queries = objectMapper.readTree(request.getFirst("queries"));

      final ObjectNode returnRoot = objectMapper.createObjectNode();
      try (final RestHighLevelClient elasticsearchClient = elasticSearchClientFactory.build()) {
        for (Iterator<String> fieldNames = queries.fieldNames(); fieldNames.hasNext(); ) {
          String field = fieldNames.next();
          final String queryText = queries.get(field).get("query").asText();
          final SearchSourceBuilder query =
              new SearchSourceBuilder().query(QueryBuilders.queryStringQuery("*" + queryText + "*").queryName(field));
          final SearchResponse response =
              elasticsearchClient.search(new SearchRequest("index").source(query), RequestOptions.DEFAULT);

          final ArrayNode results = objectMapper.createArrayNode();
          for (final SearchHit hit : response.getHits()) {
            final ObjectNode result = objectMapper.createObjectNode();
            result.put("id", hit.getId());
            result.put("score", hit.getScore() * 100);

            final JsonNode source = objectMapper.readTree(hit.getSourceAsString());
            result.put("name", source.get("title").get("value").asText());

            final ArrayNode types = objectMapper.createArrayNode();
            final String type = source.get("rdf_type").get("title").get("value").asText();
            types.add(objectMapper.createObjectNode().put("id", type).put("name", type));
            result.set("type", types);

            results.add(result);
          }

          returnRoot.set(field, objectMapper.createObjectNode().set("result", results));
        }
      }

      return Response.ok(returnRoot).build();
    } catch (JsonProcessingException e) {
      LOG.info("request not supported: {}", e.getMessage());
      return Response.status(Response.Status.BAD_REQUEST).build();
    } catch (IOException e) {
      LOG.error("Could not execute query", e);
      return Response.serverError().build();
    }

  }


}
