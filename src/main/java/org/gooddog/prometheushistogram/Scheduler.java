package org.gooddog.prometheushistogram;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class Scheduler {
  private final TestService testService;

  public Scheduler(TestService testService) {
    this.testService = testService;
  }

  @Scheduled(fixedRate = 1000)
  public void schedule() {
    testService.testApi();
    testService.getVisits();
  }
}
