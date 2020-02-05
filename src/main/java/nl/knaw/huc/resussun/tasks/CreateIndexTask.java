package nl.knaw.huc.resussun.tasks;

import io.dropwizard.servlets.tasks.Task;
import nl.knaw.huc.resussun.api.CollectionMetadata;
import nl.knaw.huc.resussun.api.CollectionsMetadataMapper;
import nl.knaw.huc.resussun.api.QueryResponse;
import nl.knaw.huc.resussun.api.QueryResponseMapper;
import nl.knaw.huc.resussun.api.Timbuctoo;
import nl.knaw.huc.resussun.api.TimbuctooException;
import nl.knaw.huc.resussun.api.TimbuctooRequest;
import nl.knaw.huc.resussun.configuration.ElasticSearchClientFactory;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CreateIndexTask extends Task {
    private static final String DATA_SET_ID = "dataSetId";
    private static final String TIMBUCTOO_URL = "timbuctooUrl";
    private static final List<String> PARAMS = List.of(DATA_SET_ID, TIMBUCTOO_URL);

    private final ElasticSearchClientFactory elasticSearchClientFactory;

    public CreateIndexTask(ElasticSearchClientFactory elasticSearchClientFactory) {
        super("createIndex");
        this.elasticSearchClientFactory = elasticSearchClientFactory;
    }

    @Override
    public void execute(Map<String, List<String>> params, PrintWriter out) throws Exception {
        if (!params.keySet().containsAll(PARAMS))
            throw new Exception("Expected parameters: " + TIMBUCTOO_URL + " and " + DATA_SET_ID);

        try (final RestHighLevelClient elasticsearchClient = elasticSearchClientFactory.build()) {
            Timbuctoo timbuctoo = new Timbuctoo(params.get(TIMBUCTOO_URL).get(0));
            String dataSetId = params.get(DATA_SET_ID).get(0);

            Map<String, List<CollectionMetadata>> collectionsMetadata = getCollectionsMetadata(timbuctoo, dataSetId);
            for (Map.Entry<String, List<CollectionMetadata>> collectionMetadata : collectionsMetadata.entrySet()) {
                List<String> props = getPropsFromMetadata(collectionMetadata.getValue());
                queryData(timbuctoo, elasticsearchClient, dataSetId, collectionMetadata.getKey(), props, null);
            }
        }
    }

    private static Map<String, List<CollectionMetadata>> getCollectionsMetadata(Timbuctoo timbuctoo, String dataSetId)
            throws TimbuctooException {
        TimbuctooRequest request = TimbuctooRequest.createCollectionsMetadataRequest(dataSetId);
        return timbuctoo.executeRequest(request, new CollectionsMetadataMapper());
    }

    private static List<String> getPropsFromMetadata(List<CollectionMetadata> collectionMetadata) {
        return collectionMetadata.stream()
                .filter(prop -> prop.getName().startsWith("rdf_type"))
                .map(prop -> {
                    String valueExpr = prop.isValueType() ? "... on Value { value }" : "... on Entity { uri }";
                    if (prop.isList())
                        valueExpr = " items { " + valueExpr + " }";
                    return prop.getName() + " { " + valueExpr + " }";
                })
                .collect(Collectors.toList());
    }

    private static void queryData(Timbuctoo timbuctoo, RestHighLevelClient elasticsearchClient,
                                  String dataSetId, String collectionId, List<String> props, String cursor)
            throws TimbuctooException, IOException {
        TimbuctooRequest request = TimbuctooRequest.createQueryRequest(dataSetId, collectionId, props, cursor);
        QueryResponse queryResponse = timbuctoo.executeRequest(request, new QueryResponseMapper());

        processData(elasticsearchClient, queryResponse.getItems());

        if (queryResponse.getNextCursor() != null)
            queryData(timbuctoo, elasticsearchClient, dataSetId, collectionId, props, queryResponse.getNextCursor());
    }

    private static void processData(RestHighLevelClient elasticsearchClient, List<Map<String, List<String>>> data)
            throws IOException {
        BulkRequest bulkRequest = new BulkRequest();
        data.iterator().forEachRemaining(entity -> {
            String uri = entity.get("uri").get(0);
            List<String> types = entity.containsKey("rdf_type") ? entity.get("rdf_type") : entity.get("rdf_typeList");
            String title = entity.get("title").get(0);

            IndexRequest indexRequest = new IndexRequest("index")
                    .id(uri)
                    .source("uri", uri, "types", types, "title", title);

            bulkRequest.add(indexRequest);
        });
        elasticsearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);
    }
}
