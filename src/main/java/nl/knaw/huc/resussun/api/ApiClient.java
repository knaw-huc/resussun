package nl.knaw.huc.resussun.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.api.StatefulRedisConnection;

public class ApiClient {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final StatefulRedisConnection<String, String> redisConnection;

  public ApiClient(StatefulRedisConnection<String, String> redisConnection) {
    this.redisConnection = redisConnection;
  }

  public boolean hasApi(String datasetId) {
    return redisConnection.sync().exists(createApiKey(datasetId)) == 1;
  }

  public ApiData getApiData(String datasetId) throws JsonProcessingException {
    String value = redisConnection.sync().get(createApiKey(datasetId));
    return (value != null) ? OBJECT_MAPPER.readValue(value, ApiData.class) : null;
  }

  public void setApiData(final ApiData apiData, String datasetId) throws JsonProcessingException {
    String value = OBJECT_MAPPER.writeValueAsString(apiData);
    redisConnection.sync().set(createApiKey(datasetId), value);
  }

  public void deleteApiData(String datasetId) {
    redisConnection.sync().del(createApiKey(datasetId));
  }

  private String createApiKey(String datasetId) {
    return "resussun:" + datasetId;
  }
}
