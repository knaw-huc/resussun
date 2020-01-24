package nl.knaw.huc.resussun;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import nl.knaw.huc.resussun.resources.RootResource;
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

  }

  @Override
  public void run(final ResussunConfiguration configuration,
                  final Environment environment) {
    environment.jersey().register(new RootResource());
    environment.jersey().register(new LoggingFeature(Logger.getLogger(LoggingFeature.DEFAULT_LOGGER_NAME), Level.INFO,
        LoggingFeature.Verbosity.PAYLOAD_ANY, LoggingFeature.DEFAULT_MAX_ENTITY_SIZE));
  }

}
