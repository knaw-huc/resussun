package nl.knaw.huc.resussun.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

@Path("/")
public class RootResource {

  public static final Logger LOG = LoggerFactory.getLogger(RootResource.class);
  private final ObjectMapper objectMapper;

  public RootResource() {
    objectMapper = new ObjectMapper();
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
      queries.fieldNames().forEachRemaining(field -> {
        returnRoot.set(field, objectMapper.createObjectNode().set("result", objectMapper.createArrayNode()));
      });

      return Response.ok(returnRoot).build();
    } catch (JsonProcessingException e) {
      LOG.info("request not supported: {}", e.getMessage());
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

  }

}
