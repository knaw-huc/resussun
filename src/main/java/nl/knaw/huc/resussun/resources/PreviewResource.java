package nl.knaw.huc.resussun.resources;

import io.dropwizard.util.Resources;
import nl.knaw.huc.resussun.api.ApiData;
import nl.knaw.huc.resussun.search.SearchClient;
import nl.knaw.huc.resussun.timbuctoo.EntityResponse;
import nl.knaw.huc.resussun.timbuctoo.EntityResponseMapper;
import nl.knaw.huc.resussun.timbuctoo.Timbuctoo;
import nl.knaw.huc.resussun.timbuctoo.TimbuctooException;
import nl.knaw.huc.resussun.timbuctoo.TimbuctooRequest;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class PreviewResource {
  private static final String HTML_TEMPLATE;

  private final SearchClient searchClient;
  private final ApiData apiData;

  static {
    try {
      URL url = Resources.getResource("preview.html");
      HTML_TEMPLATE = Resources.toString(url, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public PreviewResource(SearchClient searchClient, ApiData apiData) {
    this.searchClient = searchClient;
    this.apiData = apiData;
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  public Response get(@QueryParam("id") String id) {
    try {
      List<String> collectionIds = searchClient.getCollectionIdsForId(apiData, id);
      TimbuctooRequest request = EntityResponseMapper.createEntityRequest(
          apiData.getDataSourceId(), collectionIds.get(0), id, Collections.emptyList());

      Timbuctoo timbuctoo = apiData.getTimbuctoo();
      EntityResponse entity = timbuctoo.executeRequest(request, new EntityResponseMapper());

      String imageHtml = (entity.getImage() != null) ? "<img src=\"" + entity.getImage() + "\"/>" : "";
      String description = (entity.getDescription() != null) ? entity.getDescription() : "";

      String html = HTML_TEMPLATE
          .replace("{{title}}", entity.getTitle())
          .replace("{{image}}", imageHtml)
          .replace("{{description}}", description);

      return Response.ok(html).build();
    } catch (IOException | TimbuctooException e) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
  }
}
