package nl.knaw.huc.resussun.resources;

import nl.knaw.huc.resussun.search.SearchClient;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("preview")
public class PreviewResource {
  private final SearchClient searchClient;

  public PreviewResource(SearchClient searchClient) {
    this.searchClient = searchClient;
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  public Response get(@QueryParam("id") String id) {
    try {
      final String title = searchClient.getTitleById(id);

      if (title != null) {
        return Response.ok(createPage(title)).build();
      } else {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
    } catch (IOException e) {
      return Response.serverError().build();
    }
  }

  public String createPage(String titleById) {
    return "<html><head><meta charset=\"utf-8\" /></head>\n" +
        "<body style=\"margin: 0px; font-family: Arial; sans-serif\">\n" +
        "<div style=\"height: 300px; width: 200px; overflow: hidden; font-size: 0.7em\">\n" +
        "<p>" + titleById + "</p>" +
        "</div>\n" +
        "</body>\n" +
        "</html>";
  }
}
