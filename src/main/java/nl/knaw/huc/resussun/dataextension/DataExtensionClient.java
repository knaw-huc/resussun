package nl.knaw.huc.resussun.dataextension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.huc.resussun.api.ApiData;
import nl.knaw.huc.resussun.model.DataExtensionRequest;
import nl.knaw.huc.resussun.model.DataExtensionRequest.Property;
import nl.knaw.huc.resussun.model.DataExtensionResponse;
import nl.knaw.huc.resussun.model.DataExtensionResponse.PropertyValue;
import nl.knaw.huc.resussun.model.DataExtensionResponse.ResponseExtendProperty;
import nl.knaw.huc.resussun.timbuctoo.CollectionMetadata;
import nl.knaw.huc.resussun.timbuctoo.PropertyMetadata;
import nl.knaw.huc.resussun.timbuctoo.Timbuctoo;
import nl.knaw.huc.resussun.timbuctoo.TimbuctooException;
import nl.knaw.huc.resussun.timbuctoo.TimbuctooRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static nl.knaw.huc.resussun.timbuctoo.CollectionsMetadataMapper.collectionIdAsKey;
import static nl.knaw.huc.resussun.timbuctoo.CollectionsMetadataMapper.createCollectionsMetadataRequest;

public class DataExtensionClient {

  public DataExtensionClient() {

  }

  public JsonNode createExtensionResponse(ApiData api, DataExtensionRequest extensionRequest)
      throws TimbuctooException {
    final Timbuctoo timbuctoo = api.getTimbuctoo();

    Map<String, CollectionMetadata> dataSetMetadataMap =
        timbuctoo.executeRequest(createCollectionsMetadataRequest(api.getDataSourceId()), collectionIdAsKey());

    Map<String, Map<String, List<? extends PropertyValue>>> rows = new HashMap<>();
    if (!extensionRequest.getEntityIds().isEmpty()) {
      List<String> propertyIds = extensionRequest.getProperties().stream().map(Property::getId).collect(
          Collectors.toList());
      List<PropertyMetadata> properties = dataSetMetadataMap.values().stream()
                                                         .flatMap(col -> col.getProperties().stream())
                                                         .filter(prop -> propertyIds.contains(prop.getUri()))
                                                         .collect(Collectors.toList());

      TimbuctooExtensionQuery queryCreator = new TimbuctooExtensionQuery(
          api.getDataSourceId(),
          extensionRequest.getEntityIds(),
          properties
      );
      TimbuctooRequest timbuctooRequest = queryCreator.createQuery();
      rows = timbuctoo.executeRequest(timbuctooRequest, queryCreator.createMapper());
    }

    Map<String, String> propertyIdNameMap = createPropertyIdNameMap(dataSetMetadataMap);
    List<ResponseExtendProperty> properties = extensionRequest.getProperties().stream().map(prop -> {
      return new ResponseExtendProperty(
          prop.getId(),
          propertyIdNameMap.getOrDefault(prop.getId(), ""),
          prop.getSettings().orElse(null)
      );
    }).collect(Collectors.toList());
    return new ObjectMapper().valueToTree(new DataExtensionResponse(properties, rows));
  }

  private Map<String, String> createPropertyIdNameMap(Map<String, CollectionMetadata> dataSetMetadataMap) {
    return dataSetMetadataMap.entrySet().stream()
                             .flatMap((entry) -> entry.getValue().getProperties().stream())
                             .filter(prop -> !prop.getName().startsWith("_inverse_"))
                             .distinct()
                             .collect(Collectors.toMap(PropertyMetadata::getUri, PropertyMetadata::getName));
  }

}
