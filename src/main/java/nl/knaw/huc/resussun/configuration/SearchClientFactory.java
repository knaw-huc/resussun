package nl.knaw.huc.resussun.configuration;

import nl.knaw.huc.resussun.search.SearchClient;

public interface SearchClientFactory {
  SearchClient createSearchClient();

}
