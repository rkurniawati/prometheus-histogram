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
  private static final String TEST_SERVICE_RESPONSE_TIME_SECONDS = "test.service.response.time.seconds";
  private static final String TEST_SERVICE_QUEUE_LENGTH_HISTOGRAM = "test.service.queue.length.histogram";
  private static final String TEST_SERVICE_QUEUE_LENGTH_SUMMARY = "test.service.queue.length.summary";
  private final Timer testServiceResponseTimeSeconds;
  private final DistributionSummary testServiceQueueLengthHistogram;
  private final DistributionSummary testServiceQueueLengthSummary;
  private final NormalRandomGenerator serviceResponseTimeSecondsGenerator;
  private final PoissonRandomGenerator meanQueueLengthGenerator;

  public TestService(
      @Value("${spring.application.test.service.mean.response.time.seconds}")
          double meanResponseTimeSeconds,
      @Value("${spring.application.test.service.stddev.response.time.seconds}")
          double stddevResponseTimeSeconds,
      @Value("${spring.application.test.service.mean.queue.length}") double meanQueueLength,
      MeterRegistry meterRegistry) {

    // this will be published as a Prometheus histogram
    this.testServiceQueueLengthHistogram =
        DistributionSummary.builder(TEST_SERVICE_QUEUE_LENGTH_HISTOGRAM)
            .maximumExpectedValue(20.0)
            .publishPercentileHistogram()
            .register(meterRegistry);

    // this will be published as a Prometheus summary
    this.testServiceQueueLengthSummary =
        DistributionSummary.builder(TEST_SERVICE_QUEUE_LENGTH_SUMMARY)
            .maximumExpectedValue(20.0)
            .publishPercentiles(0.25, 0.5, 0.75, 0.95)
            .register(meterRegistry);

    // this will be published as a Prometheus histogram
    this.testServiceResponseTimeSeconds =
        Timer.builder(TEST_SERVICE_RESPONSE_TIME_SECONDS)
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
        new NormalRandomGenerator(meanResponseTimeSeconds, stddevResponseTimeSeconds);
    this.meanQueueLengthGenerator = new PoissonRandomGenerator(meanQueueLength);
  }

  public void testApi() {
    long responseTimeMillis = (long) (serviceResponseTimeSecondsGenerator.normalRandom() * 1_000);
    testServiceResponseTimeSeconds.record(Duration.ofMillis(responseTimeMillis));

    log.debug("The response time is {} ms", responseTimeMillis);
  }

  public void recordQueueLength() {
    int queueLength = meanQueueLengthGenerator.poissonRandom();
    testServiceQueueLengthHistogram.record(queueLength);
    testServiceQueueLengthSummary.record(queueLength);
    log.debug("The queue length {}", queueLength);
  }

  public void setMeanResponseTimeSeconds(double newMean) {
    serviceResponseTimeSecondsGenerator.setMean(newMean);
  }

  public void setMeanQueueLength(double mean) {
    meanQueueLengthGenerator.setMean(mean);
  }
}
