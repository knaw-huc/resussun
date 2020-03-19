package nl.knaw.huc.resussun.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.huc.resussun.api.ApiData;
import nl.knaw.huc.resussun.configuration.UrlHelperFactory;
import nl.knaw.huc.resussun.dataextension.DataExtensionClient;
import nl.knaw.huc.resussun.model.DataExtensionRequest;
import nl.knaw.huc.resussun.model.Extend;
import nl.knaw.huc.resussun.model.Preview;
import nl.knaw.huc.resussun.model.Query;
import nl.knaw.huc.resussun.model.ServiceManifest;
import nl.knaw.huc.resussun.search.SearchClient;
import nl.knaw.huc.resussun.timbuctoo.TimbuctooException;
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
  private final DataExtensionClient dataExtensionClient;

  public ApiResource(SearchClient searchClient, UrlHelperFactory urlHelperFactory) {
    this.searchClient = searchClient;
    this.urlHelperFactory = urlHelperFactory;
    dataExtensionClient = new DataExtensionClient();
  }

  @GET
  @Produces({"application/json", "application/javascript"})
  public Response get(@PathParam("api") ApiData api,
                      @QueryParam("queries") String queries,
                      @QueryParam("extend") String extend) {
    return handleRequest(api, queries, extend);
  }

  @POST
  @Consumes("application/x-www-form-urlencoded")
  @Produces({"application/json", "application/javascript"})
  public Response post(@PathParam("api") ApiData api,
                       @FormParam("queries") String queries,
                       @FormParam("extend") String extend) {
    return handleRequest(api, queries, extend);
  }

  @Path("/view")
  public ViewResource view(@PathParam("api") ApiData api) {
    return new ViewResource(searchClient, api);
  }

  @Path("/preview")
  public PreviewResource preview(@PathParam("api") ApiData api) {
    return new PreviewResource(searchClient, api);
  }

  @Path("/extend/properties")
  public DataExtensionPropertyProposalResource extend(@PathParam("api") ApiData api) {
    return new DataExtensionPropertyProposalResource(
        api.getTimbuctoo(),
        api.getDataSourceId()
    );
  }

  private Response handleRequest(ApiData api, String queriesJson, String extend) {
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
    } else if (extend != null) {
      DataExtensionRequest extensionRequest = null;
      try {
        extensionRequest = OBJECT_MAPPER.readValue(extend, DataExtensionRequest.class);
      } catch (JsonProcessingException e) {
        LOG.info("Could not parse extension request: {}", extend);
        LOG.info("Exception thrown", e);
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      try {
        return Response.ok(dataExtensionClient.createExtensionResponse(api, extensionRequest)).build();
      } catch (TimbuctooException e) {
        LOG.error("Retrieving data threw an exception", e);
        return Response.serverError().entity("Could not retrieve data").build();
      }
    }

    return Response.ok(createServiceManifest(api)).build();
  }

  private ServiceManifest createServiceManifest(ApiData api) {
    final String viewUrl = urlHelperFactory.urlHelper(api.getDataSourceId()).path("view")
                                           .queryParamTemplate("id", "{{id}}").template();
    final String previewUrl = urlHelperFactory.urlHelper(api.getDataSourceId()).path("preview")
                                              .queryParamTemplate("id", "{{id}}").template();

    return new ServiceManifest(
        String.format("Dataset \"%s\" of \"%s\" OpenRefine Recon API", api.getDataSourceId(), api.getTimbuctooUrl()),
        "http://example.org/identifierspace", "http://example.org/schemaspace")
        .viewUrl(viewUrl)
        .preview(new Preview(previewUrl, 400, 200))
        .extend(
            new Extend().proposeProperties(
                urlHelperFactory.urlHelper(api.getDataSourceId()).template(),
                "/extend/properties"
            )
        );
  }
}
