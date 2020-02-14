package nl.knaw.huc.resussun.search;

import nl.knaw.huc.resussun.model.Candidate;
import nl.knaw.huc.resussun.model.Candidates;
import nl.knaw.huc.resussun.model.Query;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SearchClient implements Closeable {
  public static final String INDEX_NAME = "index";
  private final RestHighLevelClient elasticsearchClient;

  public SearchClient(RestHighLevelClient elasticsearchClient) {
    this.elasticsearchClient = elasticsearchClient;
  }

  public Map<String, Candidates> search(Map<String, Query> queries) throws IOException {
    // Transform the queries map to a list to keep the order of the queries and their identifiers consistent
    final List<Map.Entry<String, Query>> queriesList = new ArrayList<>(queries.entrySet());

    // Map the queries to ElasticSearch search requests
    final MultiSearchRequest searchRequest = new MultiSearchRequest();
    queriesList.stream()
               .map(Map.Entry::getValue)
               .map(SearchClient::getSearchRequest)
               .forEach(searchRequest::add);

    final MultiSearchResponse response = elasticsearchClient.msearch(searchRequest, RequestOptions.DEFAULT);

    // Map the ElasticSearch search hits to candidates
    final List<Candidates> candidates =
        Arrays.stream(response.getResponses())
              .map(MultiSearchResponse.Item::getResponse)
              .map(SearchResponse::getHits)
              .map(SearchClient::getCandidates)
              .collect(Collectors.toList());

    // Zip the query identifiers and the found candidates together
    return IntStream.range(0, queriesList.size()).boxed().collect(Collectors.toMap(
        i -> queriesList.get(i).getKey(),
        candidates::get
    ));
  }

  private static SearchRequest getSearchRequest(Query query) {
    QueryBuilder builder = QueryBuilders
        .multiMatchQuery(query.getQuery())
        .type(MultiMatchQueryBuilder.Type.MOST_FIELDS)
        .fields(Map.of(
            "title", 5f, // The 'title' field is 5 times more important
            "values", 1f
        ));

    if (query.getType() != null && !query.getType().isEmpty()) {
      builder = QueryBuilders
          .boolQuery()
          .must(builder)
          .filter(QueryBuilders.termQuery("types", query.getType()));
    }

    return new SearchRequest(INDEX_NAME).source(
        new SearchSourceBuilder()
            .query(builder)
            .size((query.getLimit() != null) ? query.getLimit() : 10));
  }

  private static Candidates getCandidates(SearchHits searchHits) {
    return new Candidates(
        Arrays.stream(searchHits.getHits()).map(hit -> {
          final Map<String, Object> source = hit.getSourceAsMap();
          final Candidate candidate = new Candidate(
              hit.getId(),
              source.get("title").toString(),
              hit.getScore(),
              false
          );

          // Types are mapped to a list of strings in ElasticSearch, so we can safely cast the object to a list
          final List<String> types = (List<String>) source.get("types");
          types.forEach(type -> candidate.type(type, type));

          return candidate;
        }).collect(Collectors.toList())
    );
  }

  @Override
  public void close() throws IOException {
    elasticsearchClient.close();
  }

  public String getTitleById(String id) throws IOException {
    final GetRequest getRequest = new GetRequest(INDEX_NAME, id);
    final GetResponse response = elasticsearchClient.get(getRequest, RequestOptions.DEFAULT);
    final Map<String, Object> source = response.getSourceAsMap();

    return (String) source.get("title");
  }
}
