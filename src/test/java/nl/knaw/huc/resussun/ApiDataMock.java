package nl.knaw.huc.resussun;

import nl.knaw.huc.resussun.api.ApiData;
import nl.knaw.huc.resussun.timbuctoo.Timbuctoo;

public class ApiDataMock extends ApiData {
  private final Timbuctoo timbuctoo;

  public ApiDataMock(String dataSourceId, String timbuctooUrl, String timbuctooGuiUrl, Timbuctoo timbuctoo) {
    super(dataSourceId, timbuctooUrl, timbuctooGuiUrl);
    this.timbuctoo = timbuctoo;
  }

  public static ApiData fromApiData(ApiData apiData, Timbuctoo timbuctoo) {
    return new ApiDataMock(
        apiData.getDataSourceId(),
        apiData.getTimbuctooUrl(),
        apiData.getTimbuctooGuiUrl(),
        timbuctoo
    );
  }

  @Override
  public Timbuctoo getTimbuctoo() {
    return timbuctoo;
  }
}
