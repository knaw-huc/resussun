package nl.knaw.huc.resussun.timbuctoo;

import java.util.List;

public class QueryResponse {
  private String nextCursor;
  private List<QueryResponseItem> items;

  public QueryResponse(String nextCursor, List<QueryResponseItem> items) {
    this.nextCursor = nextCursor;
    this.items = items;
  }

  public String getNextCursor() {
    return nextCursor;
  }

  public List<QueryResponseItem> getItems() {
    return items;
  }
}
