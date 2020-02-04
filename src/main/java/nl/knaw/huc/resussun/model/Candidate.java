package nl.knaw.huc.resussun.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.HashMap;
import java.util.Map;

public class Candidate {
    @JsonProperty
    private String id;

    @JsonProperty
    private String name;

    @JsonProperty
    @JsonSerialize(using = MapToListSerializer.MapToTypeList.class)
    private Map<String, String> type;

    @JsonProperty
    private float score;

    @JsonProperty
    private boolean match;

    public Candidate(String id, String name, float score, boolean match) {
        this.id = id;
        this.name = name;
        this.score = score;
        this.match = match;
        this.type = new HashMap<>();
    }

    public Candidate type(String id, String name) {
        type.put(id, name);
        return this;
    }
}
