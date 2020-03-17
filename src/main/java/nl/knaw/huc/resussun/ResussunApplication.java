package nl.knaw.huc.resussun;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.lettuce.core.api.StatefulRedisConnection;
import nl.knaw.huc.resussun.api.ApiClient;
import nl.knaw.huc.resussun.configuration.ManagedElasticSearchClient;
import nl.knaw.huc.resussun.configuration.JsonWithPaddingInterceptor;
import nl.knaw.huc.resussun.configuration.ManagedRedisConnection;
import nl.knaw.huc.resussun.configuration.UrlHelperFactory;
import nl.knaw.huc.resussun.healthchecks.ElasticsearchHealthCheck;
import nl.knaw.huc.resussun.healthchecks.RedisHealthCheck;
import nl.knaw.huc.resussun.resources.ApiParamConverterProvider;
import nl.knaw.huc.resussun.resources.ApiResource;
import nl.knaw.huc.resussun.resources.IndexResource;
import nl.knaw.huc.resussun.search.SearchClient;
import nl.knaw.huc.resussun.tasks.CreateIndexTask;
import nl.knaw.huc.resussun.tasks.DeleteIndexTask;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.elasticsearch.client.RestHighLevelClient;
import org.glassfish.jersey.logging.LoggingFeature;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ResussunApplication extends Application<ResussunConfiguration> {

  public static void main(final String[] args) throws Exception {
    new ResussunApplication().run(args);
  }

  @Override
  public String getName() {
    return "Resussun";
  }

  @Override
  public void initialize(final Bootstrap<ResussunConfiguration> bootstrap) {
    // Make configuration properties overridable with environment variables
    // see: https://www.dropwizard.io/en/stable/manual/core.html#environment-variables
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(),
        new EnvironmentVariableSubstitutor(false)
    ));
  }

  @Override
  public void run(final ResussunConfiguration config, final Environment environment) {
    enableCors(environment);

    final ManagedElasticSearchClient managedElasticSearchClient = config.getManagedElasticSearchClient();
    final RestHighLevelClient elasticSearchClient = managedElasticSearchClient.getClient();
    final SearchClient searchClient = managedElasticSearchClient.createSearchClient();

    final ManagedRedisConnection managedRedisConnection = config.getManagedRedisConnection();
    final StatefulRedisConnection<String, String> redisConnection = managedRedisConnection.getConnection();

    final ApiClient apiClient = new ApiClient(redisConnection);
    environment.jersey().register(new ApiParamConverterProvider(apiClient));
    final UrlHelperFactory urlHelperFactory = config.getUrlHelperFactory();
    environment.jersey().register(new ApiResource(searchClient, urlHelperFactory));
    environment.jersey().register(new IndexResource(apiClient, urlHelperFactory));
    environment.jersey().register(new JsonWithPaddingInterceptor());
    environment.jersey().register(new LoggingFeature(Logger.getLogger(LoggingFeature.DEFAULT_LOGGER_NAME), Level.INFO,
        LoggingFeature.Verbosity.PAYLOAD_ANY, LoggingFeature.DEFAULT_MAX_ENTITY_SIZE));

    environment.healthChecks().register("elasticsearch", new ElasticsearchHealthCheck(elasticSearchClient));
    environment.healthChecks().register("redis", new RedisHealthCheck(redisConnection));

    environment.lifecycle().manage(managedElasticSearchClient);
    environment.lifecycle().manage(managedRedisConnection);

    environment.admin().addTask(new CreateIndexTask(elasticSearchClient, apiClient));
    environment.admin().addTask(new DeleteIndexTask(elasticSearchClient, apiClient));
  }

  private static void enableCors(final Environment environment) {
    final FilterRegistration.Dynamic cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);

    cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
    cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM,
        "X-Requested-With,Content-Type,Accept,Origin,Authorization");
    cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "OPTIONS,GET,PUT,POST,DELETE,HEAD");

    cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
  }
}
