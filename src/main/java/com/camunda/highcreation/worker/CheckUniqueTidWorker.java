package com.camunda.highcreation.worker;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class CheckUniqueTidWorker implements JobHandler {

  Logger logger = LoggerFactory.getLogger(CheckUniqueTidWorker.class);
  Map<Object, Object> uniqueTid = new HashMap<>();

  int count = 0;

  @Override
  public void handle(JobClient jobClient, ActivatedJob job) throws Exception {

    Map<String, Object> variablesAsMap = job.getVariablesAsMap();
    Object tid = variablesAsMap.get("tid");
    count++;
    if (count % 100 == 0)
      logger.info("CheckUniqueTidWorker:Managed {} jobs", count);
    synchronized (uniqueTid) {
      if (uniqueTid.containsKey(tid)) {
        logger.error("DUPLICATE PID detected {} PI:{} and {}", tid, job.getProcessDefinitionKey(), uniqueTid.get(tid));
        jobClient.newThrowErrorCommand(job)
            .errorCode("duplicateID")
            .variables(Map.of("errorCode", tid, "masterPI", uniqueTid.get(tid)))
            .errorMessage("a TID is duplicated")
            .send()
            .join();

      }
      uniqueTid.put(tid, job.getProcessDefinitionKey());
    }

    // nothing to return
    Map<String, Object> variablesToUpdate = new HashMap<>();

    // Complete the Job
    jobClient.newCompleteCommand(job.getKey()).variables(variablesToUpdate).send().join();
  }
}



