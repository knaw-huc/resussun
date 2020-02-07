package nl.knaw.huc.resussun.timbuctoo;

import com.fasterxml.jackson.databind.JsonNode;

public interface TimbuctooResponseMapper<T> {
  T mapResponse(JsonNode json);
}
