package nl.knaw.huc.resussun.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import io.lettuce.core.api.StatefulRedisConnection;

public class RedisHealthCheck extends HealthCheck {
  private final StatefulRedisConnection<String, String> connection;

  public RedisHealthCheck(StatefulRedisConnection<String, String> connection) {
    this.connection = connection;
  }

  @Override
  protected Result check() {
    return connection.sync().ping().equals("PONG") ?
        Result.builder().healthy().withMessage("Redis is available").build() :
        Result.unhealthy("Redis server is unavailable");
  }
}
