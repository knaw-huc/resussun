package nl.knaw.huc.resussun.dataextension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import nl.knaw.huc.resussun.api.ApiData;
import nl.knaw.huc.resussun.dataextension.DataExtensionRequest.Property;
import nl.knaw.huc.resussun.dataextension.DataExtensionResponse.PropertyValue;
import nl.knaw.huc.resussun.dataextension.DataExtensionResponse.ResponseExtendProperty;
import nl.knaw.huc.resussun.timbuctoo.Timbuctoo;
import nl.knaw.huc.resussun.timbuctoo.TimbuctooException;
import nl.knaw.huc.resussun.timbuctoo.TimbuctooRequest;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DataExtensionClient {
  private final Function<String, Timbuctoo> timbuctooFactory;

  public DataExtensionClient(Function<String, Timbuctoo> timbuctooFactory) {
    this.timbuctooFactory = timbuctooFactory;

  }

  public JsonNode createExtensionResponse(ApiData api, DataExtensionRequest extensionRequest)
      throws TimbuctooException {

    Map<String, Map<String, List<? extends PropertyValue>>> rows = Maps.newHashMap();
    if (!extensionRequest.getEntityIds().isEmpty()) {
      Timbuctoo timbuctoo = timbuctooFactory.apply(api.getTimbuctooUrl());

      List<String> propertyNames = extensionRequest.getProperties().stream().map(Property::getId).collect(
          Collectors.toList());
      TimbuctooExtensionQuery queryCreator = new TimbuctooExtensionQuery(
          api.getDataSourceId(),
          extensionRequest.getEntityIds(),
          propertyNames
      );
      TimbuctooRequest timbuctooRequest = queryCreator.createQuery();
      rows = timbuctoo.executeRequest(timbuctooRequest, queryCreator.createMapper());
    }

    List<ResponseExtendProperty> properties = extensionRequest.getProperties().stream().map(prop -> {
      return new ResponseExtendProperty(prop.getId(), prop.getId(), prop.getSettings().orElse(null));
    }).collect(Collectors.toList());
    return new ObjectMapper().valueToTree(new DataExtensionResponse(properties, rows));
  }

}
