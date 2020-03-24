package nl.knaw.huc.resussun.dataextension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.huc.resussun.api.ApiData;
import nl.knaw.huc.resussun.model.DataExtensionRequest;
import nl.knaw.huc.resussun.model.DataExtensionRequest.Property;
import nl.knaw.huc.resussun.model.DataExtensionResponse;
import nl.knaw.huc.resussun.model.DataExtensionResponse.PropertyValue;
import nl.knaw.huc.resussun.model.DataExtensionResponse.DataExtensionResponsePropertyMetadata;
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
                                                            .filter(prop -> propertyIds.contains(prop.getName()))
                                                            .collect(Collectors.toList());

      TimbuctooExtensionQuery queryCreator = new TimbuctooExtensionQuery(
          api.getDataSourceId(),
          extensionRequest.getEntityIds(),
          properties
      );
      TimbuctooRequest timbuctooRequest = queryCreator.createQuery();
      rows = timbuctoo.executeRequest(timbuctooRequest, queryCreator.createMapper());
    }

    final Map<String, PropertyMetadata> propertyIdPropertyMap = createPropertyIdPropertyMap(dataSetMetadataMap);
    List<DataExtensionResponsePropertyMetadata> properties = extensionRequest.getProperties().stream().map(prop -> {
      final PropertyMetadata metadata = propertyIdPropertyMap.getOrDefault(prop.getId(), null);

      if (metadata.isValueType()) {
        return DataExtensionResponse.createPropertyMetadata(
            prop.getId(),
            metadata.getUri(),
            prop.getSettings().orElse(null)
        );
      } else {
        return DataExtensionResponse.createPropertyMetadata(
            prop.getId(),
            metadata.getUri(),
            prop.getSettings().orElse(null),
            getType(metadata, dataSetMetadataMap)
        );
      }
    }).collect(Collectors.toList());
    return new ObjectMapper().valueToTree(new DataExtensionResponse(properties, rows));
  }

  private DataExtensionResponse.Type getType(PropertyMetadata metadata,
                                             Map<String, CollectionMetadata> dataSetMetadataMap) {
    final List<String> refColIds = metadata.getReferencedCollectionIds();

    return dataSetMetadataMap.entrySet().stream()
                             .filter(entry -> refColIds.contains(entry.getKey()))
                             .map(Map.Entry::getValue)
                             .map(col -> new DataExtensionResponse.Type(col.getUri(), col.getCollectionId()))
                             .findFirst().orElse(null);
  }


  private Map<String, PropertyMetadata> createPropertyIdPropertyMap(
      Map<String, CollectionMetadata> dataSetMetadataMap) {
    return dataSetMetadataMap.entrySet().stream()
                             .flatMap((entry) -> entry.getValue().getProperties().stream())
                             .distinct()
                             .collect(Collectors.toMap(PropertyMetadata::getName, prop -> prop));
  }

}
