package nl.knaw.huc.resussun.timbuctoo;

import java.util.List;
import java.util.Map;

public class EntityResponse {
  private String uri;
  private String title;
  private String description;
  private String image;
  private Map<String, List<String>> values;

  public EntityResponse(String uri, String title, String description,
                        String image, Map<String, List<String>> values) {
    this.uri = uri;
    this.title = title;
    this.description = description;
    this.image = image;
    this.values = values;
  }

  public String getUri() {
    return uri;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public String getImage() {
    return image;
  }

  public Map<String, List<String>> getValues() {
    return values;
  }
}
