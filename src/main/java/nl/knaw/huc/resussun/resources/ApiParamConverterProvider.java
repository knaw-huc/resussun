package nl.knaw.huc.resussun.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import nl.knaw.huc.resussun.api.ApiClient;
import nl.knaw.huc.resussun.api.ApiData;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class ApiParamConverterProvider implements ParamConverterProvider {
  private final ApiClient apiClient;

  public ApiParamConverterProvider(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  @Override
  public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
    if (rawType == ApiData.class) {
      return (ParamConverter<T>) new ApiParamConverter(apiClient);
    }

    return null;
  }

  private static final class ApiParamConverter implements ParamConverter<ApiData> {
    private final ApiClient apiClient;

    public ApiParamConverter(ApiClient apiClient) {
      this.apiClient = apiClient;
    }

    @Override
    public ApiData fromString(String datasetId) {
      try {
        if (!this.apiClient.hasApi(datasetId)) {
          Response response = Response
              .status(Response.Status.NOT_FOUND)
              .entity("No API for " + datasetId)
              .build();

          throw new WebApplicationException(response);
        }

        return this.apiClient.getApiData(datasetId);
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
