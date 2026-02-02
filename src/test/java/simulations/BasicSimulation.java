package simulations;

import java.time.Duration;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class BasicSimulation extends Simulation {
  private final HttpProtocolBuilder httpProtocol = http
    .baseUrl("https://httpbin.org/#/")
    .acceptHeader("application/json")
    // Auto-load embedded resources, but skip common static assets and third-party domains
    .inferHtmlResources(
      // Exclude common static file extensions OR third-party domains
      DenyList(".*\\.(?:css|js|png|jpg|jpeg|gif|svg|ico|woff2?|ttf|eot)(?:\\?.*)?$|https?://(?:www\\.)?(?:google-analytics\\.com|googletagmanager\\.com|doubleclick\\.net|gstatic\\.com|facebook\\.net|hotjar\\.com)/.*")
    );

  private final FeederBuilder.FileBased<String> feeder = csv("data/users.csv").circular();

  private final ScenarioBuilder scn = scenario("Basic Scenario")
    .feed(feeder)
    .exec(
      http("Get Delay")
        .get("/delay/1")
        .check(status().is(200))
    )
    .pause(1);

  {
    setUp(
      scn.injectOpen(rampUsers(10).during(Duration.ofSeconds(10)))
    ).protocols(httpProtocol);
  }
}
