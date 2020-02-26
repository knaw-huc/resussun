package nl.knaw.huc.resussun.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.lifecycle.Managed;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;

import java.time.Duration;

public class ManagedRedisConnection implements Managed {
  private final RedisClient client;
  private final StatefulRedisConnection<String, String> connection;

  @JsonCreator
  public ManagedRedisConnection(@JsonProperty("hostName") String hostName, @JsonProperty("port") int port) {
    client = RedisClient.create(new RedisURI(hostName, port, Duration.ofSeconds(10)));
    connection = client.connect();
  }

  public StatefulRedisConnection<String, String> getConnection() {
    return connection;
  }

  @Override
  public void start() {
  }

  @Override
  public void stop() {
    connection.close();
    client.shutdown();
  }
}
