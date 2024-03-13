package com.camunda.highcreation.worker;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenerateTrafficWorker implements JobHandler {

  Logger logger = LoggerFactory.getLogger(GenerateTrafficWorker.class);
  int count = 0;

  @Override
  public void handle(JobClient jobClient, ActivatedJob job) throws Exception {
    count++;
    if (count % 100 == 0)
      logger.info("GenerateTrafficWorker:Managed {} jobs", count);

    // Complete the Job
    jobClient.newCompleteCommand(job.getKey()).send().join();
  }
}

