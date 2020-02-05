package nl.knaw.huc.resussun.api;

import java.util.List;
import java.util.Map;

public class QueryResponse {
    private String nextCursor;
    private List<Map<String, List<String>>> items;

    public QueryResponse(String nextCursor, List<Map<String, List<String>>> items) {
        this.nextCursor = nextCursor;
        this.items = items;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public List<Map<String, List<String>>> getItems() {
        return items;
    }
}
