package nl.knaw.huc.resussun.timbuctoo;

import java.util.List;
import java.util.Objects;

public class CollectionMetadata {
  private final String collectionId;
  private final String collectionListId;
  private final String uri;
  private final List<PropertyMetadata> properties;

  public CollectionMetadata(String collectionId,
                            String collectionListId,
                            String uri,
                            List<PropertyMetadata> properties) {
    this.collectionId = collectionId;
    this.collectionListId = collectionListId;
    this.uri = uri;
    this.properties = properties;
  }

  public String getCollectionId() {
    return collectionId;
  }

  public String getCollectionListId() {
    return collectionListId;
  }

  public String getUri() {
    return uri;
  }

  public List<PropertyMetadata> getProperties() {
    return properties;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    CollectionMetadata that = (CollectionMetadata) obj;
    return Objects.equals(collectionId, that.collectionId) &&
        Objects.equals(collectionListId, that.collectionListId) &&
        Objects.equals(uri, that.uri) &&
        Objects.equals(properties, that.properties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(collectionId, collectionListId, uri, properties);
  }
}
