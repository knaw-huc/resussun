package nl.knaw.huc.resussun.resources;

import nl.knaw.huc.resussun.ApiDataMock;
import nl.knaw.huc.resussun.api.ApiData;
import nl.knaw.huc.resussun.timbuctoo.Timbuctoo;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class ApiParamConverterProviderMock implements ParamConverterProvider {
  public static final ApiData API_DATA = new ApiData("test", "http://timbuctoo", "http://timbuctoo-gui");
  public ApiData apiData;

  public ApiParamConverterProviderMock(ApiData apiData) {
    this.apiData = apiData;
  }

  public ApiParamConverterProviderMock() {
    this(API_DATA);
  }

  @Override
  public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
    if (rawType == ApiData.class) {
      return new ParamConverter<T>() {
        @Override
        public T fromString(String value) {
          return (T) (apiData.getDataSourceId().equals(value) ? apiData : null);
        }

        @Override
        public String toString(T value) {
          return apiData.getDataSourceId();
        }
      };
    }

    return null;
  }
}
