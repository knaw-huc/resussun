package nl.knaw.huc.resussun;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import nl.knaw.huc.resussun.configuration.ElasticSearchClientFactory;
import nl.knaw.huc.resussun.healthchecks.ElasticsearchHealthCheck;
import nl.knaw.huc.resussun.resources.RootResource;
import nl.knaw.huc.resussun.tasks.CreateIndexTask;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.glassfish.jersey.logging.LoggingFeature;

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
  public void run(final ResussunConfiguration config,
                  final Environment environment) {
    final ElasticSearchClientFactory elasticsearchClientFactory = config.getElasticSearchClientFactory();
    environment.jersey().register(new RootResource(elasticsearchClientFactory));
    environment.jersey().register(new LoggingFeature(Logger.getLogger(LoggingFeature.DEFAULT_LOGGER_NAME), Level.INFO,
      LoggingFeature.Verbosity.PAYLOAD_ANY, LoggingFeature.DEFAULT_MAX_ENTITY_SIZE));
    environment.healthChecks().register("elasticsearch", new ElasticsearchHealthCheck(elasticsearchClientFactory));

    environment.admin().addTask(new CreateIndexTask(elasticsearchClientFactory));
  }

}
