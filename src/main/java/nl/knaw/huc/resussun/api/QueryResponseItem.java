package nl.knaw.huc.resussun.api;

import java.util.List;
import java.util.Map;

public class QueryResponseItem {
    private String uri;
    private String title;
    private List<String> types;
    private Map<String, List<String>> values;

    public QueryResponseItem(String uri, String title, List<String> types, Map<String, List<String>> values) {
        this.uri = uri;
        this.title = title;
        this.types = types;
        this.values = values;
    }

    public String getUri() {
        return uri;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getTypes() {
        return types;
    }

    public Map<String, List<String>> getValues() {
        return values;
    }
}
