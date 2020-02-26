package nl.knaw.huc.resussun.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.lettuce.core.api.StatefulRedisConnection;
import nl.knaw.huc.resussun.api.ApiClient;
import nl.knaw.huc.resussun.api.ApiData;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class ApiParamConverterProvider implements ParamConverterProvider {
  private final StatefulRedisConnection<String, String> redisConnection;

  public ApiParamConverterProvider(StatefulRedisConnection<String, String> redisConnection) {
    this.redisConnection = redisConnection;
  }

  @Override
  public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
    if (rawType == ApiData.class) {
      return (ParamConverter<T>) new ApiParamConverter(redisConnection);
    }

    return null;
  }

  private static final class ApiParamConverter implements ParamConverter<ApiData> {
    private final StatefulRedisConnection<String, String> redisConnection;

    public ApiParamConverter(StatefulRedisConnection<String, String> redisConnection) {
      this.redisConnection = redisConnection;
    }

    @Override
    public ApiData fromString(String value) {
      try {
        ApiClient apiClient = new ApiClient(redisConnection, value);
        if (!apiClient.hasApi()) {
          Response response = Response
              .status(Response.Status.NOT_FOUND)
              .entity("No API for " + value)
              .build();

          throw new WebApplicationException(response);
        }

        return apiClient.getApiData();
      } catch (JsonProcessingException e) {
        throw new IllegalArgumentException(e);
      }
    }

    @Override
    public String toString(ApiData value) {
      return value.getDataSourceId();
    }
  }
}
