package simulations;

import java.time.Duration;
import java.util.Locale;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class BasicSimulation extends Simulation {
  private static final String BASE_URL = System.getProperty("test.baseUrl", "https://www.starlux-airlines.com/flights/zh-tw/");
  private static final String CACHE_PATH = System.getProperty("test.cachePath", "/cache/60");
  private static final int WARM_REQUESTS = parsePositiveInt(System.getProperty("test.warmRequests"), 2);

  private final HttpProtocolBuilder httpProtocol = http
    .baseUrl(BASE_URL)
    .acceptHeader("application/json")
    // Auto-load embedded resources, but skip common static assets and third-party domains
    .inferHtmlResources(
      // Exclude common static file extensions OR third-party domains
      DenyList(".*\\.(?:css|js|png|jpg|jpeg|gif|svg|ico|woff2?|ttf|eot)(?:\\?.*)?$|https?://(?:www\\.)?(?:google-analytics\\.com|googletagmanager\\.com|doubleclick\\.net|gstatic\\.com|facebook\\.net|hotjar\\.com)/.*")
    );

  private final FeederBuilder.FileBased<String> feeder = csv("data/users.csv").circular();

  private final ScenarioBuilder scn = buildScenario();

  private ScenarioBuilder buildScenario() {
    ScenarioBuilder builder = scenario("CDN Verification Scenario")
      .feed(feeder);

    for (int i = 1; i <= WARM_REQUESTS; i++) {
      builder = builder.exec(
        http("Warm_" + i)
          .get(CACHE_PATH)
          .check(status().in(200, 304))
      );
    }

    return builder
      .exec(
        http("CDN_Probe_1")
          .get(CACHE_PATH)
          .check(
            status().in(200, 304),
            header("x-cdn").optional().saveAs(key("p1", "h_x_cdn")),
            header("x-iinfo").optional().saveAs(key("p1", "h_x_iinfo")),
            header("cf-ray").optional().saveAs(key("p1", "h_cf_ray")),
            header("age").optional().saveAs(key("p1", "h_age")),
            header("x-cache").optional().saveAs(key("p1", "h_x_cache")),
            header("x-cache-hits").optional().saveAs(key("p1", "h_x_cache_hits")),
            header("cf-cache-status").optional().saveAs(key("p1", "h_cf_cache_status"))
          )
      )
      .exec(session -> evaluateProbe(session, "p1", "Probe-1"))
      .pause(1)
      .exec(
        http("CDN_Probe_2")
          .get(CACHE_PATH)
          .check(
            status().in(200, 304),
            header("x-cdn").optional().saveAs(key("p2", "h_x_cdn")),
            header("x-iinfo").optional().saveAs(key("p2", "h_x_iinfo")),
            header("cf-ray").optional().saveAs(key("p2", "h_cf_ray")),
            header("age").optional().saveAs(key("p2", "h_age")),
            header("x-cache").optional().saveAs(key("p2", "h_x_cache")),
            header("x-cache-hits").optional().saveAs(key("p2", "h_x_cache_hits")),
            header("cf-cache-status").optional().saveAs(key("p2", "h_cf_cache_status"))
          )
      )
      .exec(session -> evaluateProbe(session, "p2", "Probe-2"))
      .exec(BasicSimulation::printFinalResult)
      .pause(1);
  }

  private static Session evaluateProbe(Session session, String prefix, String label) {
    String xCdn = sessionText(session, key(prefix, "h_x_cdn"));
    String xIinfo = sessionText(session, key(prefix, "h_x_iinfo"));
    String cfRay = sessionText(session, key(prefix, "h_cf_ray"));
    String age = sessionText(session, key(prefix, "h_age"));
    String xCache = sessionText(session, key(prefix, "h_x_cache"));
    String xCacheHits = sessionText(session, key(prefix, "h_x_cache_hits"));
    String cfCacheStatus = sessionText(session, key(prefix, "h_cf_cache_status"));

    boolean cdnInPath = hasText(xCdn) || hasText(xIinfo) || hasText(cfRay);
    boolean cdnCacheHit =
      parsePositiveInt(age, 0) > 0
        || containsIgnoreCase(xCache, "HIT")
        || parsePositiveInt(xCacheHits, 0) > 0
        || equalsIgnoreCase(cfCacheStatus, "HIT");
    boolean strictHit = cdnInPath && cdnCacheHit;

    System.out.println("[CDN][" + label + "] header values: x-cdn=" + safe(xCdn)
      + ", x-iinfo=" + safe(xIinfo)
      + ", cf-ray=" + safe(cfRay)
      + ", age=" + safe(age)
      + ", x-cache=" + safe(xCache)
      + ", x-cache-hits=" + safe(xCacheHits)
      + ", cf-cache-status=" + safe(cfCacheStatus));
    System.out.println("[CDN][" + label + "] cdn_in_path=" + cdnInPath
      + ", cdn_cache_hit=" + cdnCacheHit
      + ", strictHit=" + strictHit);

    return session
      .set(key(prefix, "cdn_in_path"), String.valueOf(cdnInPath))
      .set(key(prefix, "cdn_cache_hit"), String.valueOf(cdnCacheHit))
      .set(key(prefix, "strict_hit"), String.valueOf(strictHit));
  }

  private static Session printFinalResult(Session session) {
    boolean strictHit1 = parseBoolean(sessionText(session, key("p1", "strict_hit")));
    boolean strictHit2 = parseBoolean(sessionText(session, key("p2", "strict_hit")));
    boolean strictHitAny = strictHit1 || strictHit2;

    System.out.println("[CDN][Final] strict_hit_probe1=" + strictHit1
      + ", strict_hit_probe2=" + strictHit2
      + ", strict_hit_any=" + strictHitAny);
    return session;
  }

  private static String key(String prefix, String name) {
    return prefix + "_" + name;
  }

  private static String sessionText(Session session, String key) {
    return session.contains(key) ? session.getString(key) : "";
  }

  private static String safe(String value) {
    return hasText(value) ? value : "-";
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private static boolean parseBoolean(String value) {
    return hasText(value) && Boolean.parseBoolean(value.trim());
  }

  private static int parsePositiveInt(String value, int fallback) {
    if (!hasText(value)) {
      return fallback;
    }
    try {
      int parsed = Integer.parseInt(value.trim());
      return parsed >= 0 ? parsed : fallback;
    } catch (NumberFormatException ignored) {
      return fallback;
    }
  }

  private static boolean containsIgnoreCase(String value, String token) {
    return hasText(value) && value.toLowerCase(Locale.ROOT).contains(token.toLowerCase(Locale.ROOT));
  }

  private static boolean equalsIgnoreCase(String value, String expected) {
    return hasText(value) && value.trim().equalsIgnoreCase(expected);
  }

  {
    System.out.println("[CDN][Config] baseUrl=" + BASE_URL
      + ", cachePath=" + CACHE_PATH
      + ", warmRequests=" + WARM_REQUESTS);

    setUp(
      scn.injectOpen(rampUsers(10).during(Duration.ofSeconds(10)))
    ).protocols(httpProtocol);
  }
}
