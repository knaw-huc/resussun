package nl.knaw.huc.resussun.resources;

import nl.knaw.huc.resussun.api.ApiData;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class ApiParamConverterProviderMock implements ParamConverterProvider {
  public static final ApiData API_DATA = new ApiData("test",
      "http://timbuctoo", "http://timbuctoo-gui");

  @Override
  public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
    if (rawType == ApiData.class) {
      return new ParamConverter<T>() {
        @Override
        public T fromString(String value) {
          return (T) (API_DATA.getDataSourceId().equals(value) ? API_DATA : null);
        }

        @Override
        public String toString(T value) {
          return API_DATA.getDataSourceId();
        }
      };
    }

    return null;
  }
}
