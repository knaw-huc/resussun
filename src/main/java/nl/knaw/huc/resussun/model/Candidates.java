package nl.knaw.huc.resussun.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class Candidates {
    @JsonProperty
    private List<Candidate> result = new ArrayList<>();

    public Candidates candidate(Candidate candidate) {
        result.add(candidate);
        return this;
    }
}
