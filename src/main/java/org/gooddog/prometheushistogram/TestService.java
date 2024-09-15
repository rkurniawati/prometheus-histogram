package org.gooddog.prometheushistogram;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.gooddog.prometheushistogram.generators.NormalRandomGenerator;
import org.gooddog.prometheushistogram.generators.PoissonRandomGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TestService {
  private static final String RESPONSE_TIME_METRIC_NAME = "test.service.response.time.seconds";
  private static final String VISITS_METRIC_NAME = "test.service.visits";
  private static final String VISITS2_METRIC_NAME = "test.service.visits2";
  private final Timer testServiceResponseTimeSeconds;
  private final DistributionSummary testServiceVisitHistogram;
  private final DistributionSummary testServiceVisitSummary;
  private final NormalRandomGenerator serviceResponseTimeSecondsGenerator;
  private final PoissonRandomGenerator serviceVisitsGenerator;

  public TestService(
      @Value("${spring.application.test.service.mean.response.time.seconds}")
          double serviceMeanResponseTimeSeconds,
      @Value("${spring.application.test.service.stddev.response.time.seconds}")
          double servicestddevResponseTimeSeconds,
      @Value("${spring.application.test.service.mean.visits}") double serviceVisitMeans,
      MeterRegistry meterRegistry) {

    // this will be published as a Prometheus histogram
    this.testServiceVisitHistogram =
        DistributionSummary.builder(VISITS_METRIC_NAME)
            .maximumExpectedValue(20.0)
            .publishPercentileHistogram()
            .register(meterRegistry);

    // this will be published as a Prometheus summary
    this.testServiceVisitSummary =
        DistributionSummary.builder(VISITS2_METRIC_NAME)
            .maximumExpectedValue(20.0)
            .publishPercentiles(0.25, 0.5, 0.75, 0.95)
            .register(meterRegistry);

    // this will be published as a Prometheus histogram
    this.testServiceResponseTimeSeconds =
        Timer.builder(RESPONSE_TIME_METRIC_NAME)
            .serviceLevelObjectives(
                Duration.ofMillis(100),
                Duration.ofMillis(200),
                Duration.ofMillis(300),
                Duration.ofMillis(400),
                Duration.ofMillis(500),
                Duration.ofMillis(600),
                Duration.ofMillis(700),
                Duration.ofMillis(800),
                Duration.ofMillis(900),
                Duration.ofMillis(1000),
                Duration.ofMillis(1100),
                Duration.ofMillis(1200),
                Duration.ofMillis(1300),
                Duration.ofMillis(1400),
                Duration.ofMillis(1500))
            .maximumExpectedValue(Duration.ofMillis(1500))
            .register(meterRegistry);
    this.serviceResponseTimeSecondsGenerator =
        new NormalRandomGenerator(serviceMeanResponseTimeSeconds, servicestddevResponseTimeSeconds);
    this.serviceVisitsGenerator = new PoissonRandomGenerator(serviceVisitMeans);
  }

  public void testApi() {
    long responseTimeMillis = (long) (serviceResponseTimeSecondsGenerator.normalRandom() * 1_000);
    testServiceResponseTimeSeconds.record(Duration.ofMillis(responseTimeMillis));

    log.debug("The response time is {} ms", responseTimeMillis);
  }

  public void getVisits() {
    int visits = serviceVisitsGenerator.poissonRandom();
    testServiceVisitHistogram.record(visits);
    testServiceVisitSummary.record(visits);
    log.debug("The number of Visits {}", visits);
  }

  public void setServiceMeanResponseTimeSeconds(double newMean) {
    serviceResponseTimeSecondsGenerator.setMean(newMean);
  }

  public void setServiceVisitMeans(double mean) {
    serviceVisitsGenerator.setMean(mean);
  }
}
