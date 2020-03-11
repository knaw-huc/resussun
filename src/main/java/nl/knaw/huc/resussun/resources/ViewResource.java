package nl.knaw.huc.resussun.resources;

import nl.knaw.huc.resussun.api.ApiData;
import nl.knaw.huc.resussun.search.SearchClient;

import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.List;

public class ViewResource {
  private final SearchClient searchClient;
  private final ApiData apiData;

  public ViewResource(SearchClient searchClient, ApiData apiData) {
    this.searchClient = searchClient;
    this.apiData = apiData;
  }

  @GET
  public Response view(@QueryParam("id") String id) {
    try {
      List<String> collectionIds = searchClient.getCollectionIdsForId(apiData.getDataSourceId(), id);
      URI uri = apiData.getTimbuctooGuiUrlHelper()
                       .path("details")
                       .path(apiData.getDataSourceId())
                       .path(collectionIds.get(0))
                       .path(id)
                       .toUri();
      return Response.seeOther(uri).build();
    } catch (IOException e) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
  }
}
