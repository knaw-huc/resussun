package nl.knaw.huc.resussun.resources;

import nl.knaw.huc.resussun.model.PropertyProposal;
import nl.knaw.huc.resussun.model.PropertyProposal.Property;
import nl.knaw.huc.resussun.timbuctoo.CollectionMetadata;
import nl.knaw.huc.resussun.timbuctoo.Timbuctoo;
import nl.knaw.huc.resussun.timbuctoo.TimbuctooException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static nl.knaw.huc.resussun.timbuctoo.CollectionsMetadataMapper.createCollectionsMetadataRequest;
import static nl.knaw.huc.resussun.timbuctoo.CollectionsMetadataMapper.uriAsKey;

public class DataExtensionPropertyProposalResource {
  private static final Logger LOG = LoggerFactory.getLogger(DataExtensionPropertyProposalResource.class);
  private final Timbuctoo timbuctoo;
  private final String dataSourceId;

  public DataExtensionPropertyProposalResource(Timbuctoo timbuctoo, String dataSourceId) {
    this.timbuctoo = timbuctoo;
    this.dataSourceId = dataSourceId;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response properties(@QueryParam("type") String typeId, @QueryParam("limit") @Nullable Integer limit) {
    if (StringUtils.isBlank(typeId)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Query param \"type\" is required.").build();
    }

    try {
      final Map<String, CollectionMetadata> collectionMetadata = timbuctoo.executeRequest(
          createCollectionsMetadataRequest(dataSourceId),
          uriAsKey()
      );
      if (!collectionMetadata.containsKey(typeId)) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }

      final List<Property> props = getPropertiesForCollection(typeId, collectionMetadata);

      return Response.ok(new PropertyProposal(typeId, props)).build();

    } catch (TimbuctooException e) {
      LOG.error("Retrieve data from Timbuctoo failed", e);
      return Response.serverError().entity("Could not retrieve metadata").build();
    }
  }

  private List<Property> getPropertiesForCollection(
      String typeId,
      Map<String, CollectionMetadata> collectionMetadata
  ) {
    return collectionMetadata.get(typeId).getProperties().stream()
                             .filter(prop -> !prop.getName().equals("rdf_type"))
                             .map(propmd -> new Property(propmd.getUri(), propmd.getName()))
                             .collect(Collectors.toList());
  }
}
