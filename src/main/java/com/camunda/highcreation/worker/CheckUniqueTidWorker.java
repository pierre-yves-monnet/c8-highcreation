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
  private final Map<Object, RegisterTID> uniqueTid = new HashMap<>();

  int count = 0;
  int countDuplicate = 0;
  int countReplay = 0;
  @Override
  public void handle(JobClient jobClient, ActivatedJob job) throws Exception {

    Map<String, Object> variablesAsMap = job.getVariablesAsMap();
    Object tid = variablesAsMap.get("tid");
    count++;
    if (count % 100 == 0)
      logger.info("CheckUniqueTidWorker:Managed {} jobs", count);
    synchronized (uniqueTid) {
      if (uniqueTid.containsKey(tid)) {
        RegisterTID registerTID = uniqueTid.get(tid);
        if (registerTID.jobId == job.getKey()) {
          // false positive: we replay the same job
          countReplay++;
          logger.error(" #{} REPLAY TID detected {}", countReplay, tid);
        } else {
          countDuplicate++;
          logger.error(">>>>>>>>> #{} DUPLICATE PID detected {} PI:{} and {}", countDuplicate, tid, job.getProcessInstanceKey(),
              registerTID.processInstance);
          jobClient.newThrowErrorCommand(job)
              .errorCode("duplicateID")
              .variables(Map.of("errorCode", tid, "masterPI", uniqueTid.get(tid)))
              .errorMessage("a TID is duplicated")
              .send()
              .join();
          return;
        }
      }
      RegisterTID registerTID = new RegisterTID();
      registerTID.jobId = job.getKey();
      registerTID.processInstance = job.getProcessInstanceKey();
      uniqueTid.put(tid, registerTID);

    }

    // nothing to return
    Map<String, Object> variablesToUpdate = new HashMap<>();

    // Complete the Job
    try {
      jobClient.newCompleteCommand(job.getKey()).variables(variablesToUpdate).send().join();
    } catch (Exception e) {
      logger.debug("Error during complete job {}", e.getMessage());
    }
  }

  private static class RegisterTID {
    public long processInstance;
    public long jobId;
  }
}



