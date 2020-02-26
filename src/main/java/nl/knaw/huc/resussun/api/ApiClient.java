package nl.knaw.huc.resussun.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.api.StatefulRedisConnection;

public class ApiClient {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final StatefulRedisConnection<String, String> redisConnection;
  private final String key;

  public ApiClient(StatefulRedisConnection<String, String> redisConnection, String datasetId) {
    this.redisConnection = redisConnection;
    this.key = "resussun:" + datasetId;
  }

  public boolean hasApi() {
    return redisConnection.sync().exists(key) == 1;
  }

  public ApiData getApiData() throws JsonProcessingException {
    String value = redisConnection.sync().get(key);
    return (value != null) ? OBJECT_MAPPER.readValue(value, ApiData.class) : null;
  }

  public void setApiData(final ApiData apiData) throws JsonProcessingException {
    String value = OBJECT_MAPPER.writeValueAsString(apiData);
    redisConnection.sync().set(key, value);
  }

  public void deleteApiData() {
    redisConnection.sync().del(key);
  }
}
