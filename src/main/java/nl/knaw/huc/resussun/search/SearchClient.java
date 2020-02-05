package nl.knaw.huc.resussun.search;

import nl.knaw.huc.resussun.model.Candidate;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SearchClient implements Closeable {
  private final RestHighLevelClient elasticsearchClient;

  public SearchClient(RestHighLevelClient elasticsearchClient) {
    this.elasticsearchClient = elasticsearchClient;
  }

  public void search(String queryText, String field, Consumer<Candidate> candidates)
      throws IOException {
    final SearchSourceBuilder query =
        new SearchSourceBuilder().query(QueryBuilders.queryStringQuery("*" + queryText + "*").queryName(field));
    final SearchResponse response =
        elasticsearchClient.search(new SearchRequest("index").source(query), RequestOptions.DEFAULT);


    for (final SearchHit hit : response.getHits()) {
      final Map<String, Object> source = hit.getSourceAsMap();
      final Candidate candidate = new Candidate(
          hit.getId(),
          source.get("title").toString(),
          hit.getScore() * 100,
          false
      );

      List<String> types = (List<String>) source.get("types");
      types.forEach(type -> candidate.type(type, type));

      candidates.accept(candidate);
    }
  }

  @Override
  public void close() throws IOException {
    elasticsearchClient.close();
  }
}
