package nl.knaw.huc.resussun.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Candidates {
  @JsonProperty
  private List<Candidate> result;

  public Candidates(List<Candidate> results) {
    this.result = results;
  }
}
