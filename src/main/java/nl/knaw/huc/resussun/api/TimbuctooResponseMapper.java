package nl.knaw.huc.resussun.api;

import com.fasterxml.jackson.databind.JsonNode;

public interface TimbuctooResponseMapper<T> {
    T mapResponse(JsonNode json);
}
