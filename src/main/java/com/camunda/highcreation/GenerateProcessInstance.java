package com.camunda.highcreation;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.command.CreateProcessInstanceCommandStep1;
import io.camunda.zeebe.client.api.response.ProcessInstanceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class GenerateProcessInstance {
  static Logger logger = LoggerFactory.getLogger(GenerateProcessInstance.class);

  int numberOfProcessInstance;
  ZeebeClient zeebeClient;
  String base;
  boolean withResult;
  int numberOfSecondToWait;
  int threadsInParallel;
  String processId;
  long beginTimeOperation;
  String tenantId;

  HashMap<String, Object> registerCreation = new HashMap<>();

  GenerateProcessInstance(ZeebeClient zeebeClient,
                          int numberOfProcess,
                          boolean withResult,
                          int numberOfSecondToWait,
                          int threadsInParallel,
                          String processId,
                          String tenantId,
                          long beginTimeOperation) {
    this.zeebeClient = zeebeClient;
    this.numberOfProcessInstance = numberOfProcess;
    this.withResult = withResult;
    this.numberOfSecondToWait = numberOfSecondToWait;
    this.processId = processId;
    this.beginTimeOperation = beginTimeOperation;
    this.tenantId = tenantId;
    this.threadsInParallel = threadsInParallel;

  }

  /**
   * Create a process intances
   */
  public void createProcessInstances() {
    long beg = System.currentTimeMillis();

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // Format the date and time into a string
    base = LocalDateTime.now().format(formatter);

    logger.info("Start generate {} processId [{}] tenantId[{}]", numberOfProcessInstance, processId, tenantId);
    ExecutorService executor = Executors.newFixedThreadPool(this.threadsInParallel);

    for (int i = 0; i < numberOfProcessInstance; i++) {
      try {

        executor.execute(new StartProcessInstance(i, this));

      } catch (Exception e) {
        logger.error("ERROR Create PI in Process[{}] Tenant[{}] : {}", processId, tenantId, e.getMessage());
      }
    }

    long end = System.currentTimeMillis();

    logger.info("Generate in ThreadPool {} processInstances in {} ms ", numberOfProcessInstance, end - beg);

    // please wait the queue is empty
    if (executor instanceof ThreadPoolExecutor) {
      ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;

      while (threadPoolExecutor.getQueue().size() > 0){
        logger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Queue size {} ~~~~~~~~~~~~~~~~~~", threadPoolExecutor.getQueue().size());
        try {
          Thread.sleep(10000);
        } catch (InterruptedException e) {
        }
      }
      logger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Queue is empty! ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
      executor.shutdown();
    }


  }

  /**
   * StartProcessinstance in a runnable
   */
  static class StartProcessInstance implements Runnable {
    int index;
    GenerateProcessInstance generateProcessInstance;

    StartProcessInstance(int index, GenerateProcessInstance generateProcessInstance) {
      this.index = index;
      this.generateProcessInstance = generateProcessInstance;
    }

    @Override
    public void run() {
      List<Integer> listTraffic = new ArrayList<>();
      for (int i = 0; i < 20; i++) {
        listTraffic.add(i);
      }

      // This value must be unique, and not already generated
      String tid = generateProcessInstance.base + "-" + index;

      // We register this tid
      synchronized (generateProcessInstance) {
        if (generateProcessInstance.registerCreation.containsKey(tid)) {
          logger.error(">>>>>>> Generate : duplicate TID, so we don't create this one");
          return;
        }
        // Just register Long(0) for the moment, because we will not have the correct PID before the withResult() and the duplication will be detected before
        generateProcessInstance.registerCreation.put(tid, Long.valueOf(0));
      }

      Map<String, Object> variables = new HashMap<>();
      variables.put("tid", tid);
      variables.put("listTraffic", listTraffic);


      CreateProcessInstanceCommandStep1.CreateProcessInstanceCommandStep3 processInstanceStep3 = generateProcessInstance.zeebeClient.newCreateInstanceCommand()
          .bpmnProcessId(generateProcessInstance.processId)
          .latestVersion()
          .variables(variables);

      if (generateProcessInstance.tenantId != null) {
        processInstanceStep3 = processInstanceStep3.tenantId(generateProcessInstance.tenantId);
      }

      // ------------------ withResult
      if (generateProcessInstance.withResult) {

        long startTime = System.currentTimeMillis();
        ProcessInstanceResult result = null;
        String exception = null;
        try {
          result = processInstanceStep3.withResult()
              .requestTimeout(Duration.ofMillis(1000L * generateProcessInstance.numberOfSecondToWait))
              .send()
              .join();
          // generateProcessInstance.zeebeClient.newSetVariablesCommand(result.getProcessInstanceKey()).variables(variables).send();

        } catch (Exception e) {
          exception = e.getMessage();
        }
        long endTime = System.currentTimeMillis();
        // we give 150 ms margin, time to get the answer from Zeebe
        if (endTime - startTime > 150L + 1000L * generateProcessInstance.numberOfSecondToWait) {
          logger.error("OVERTIME the timeout {}s : {} ms result? [{}] exception? [{}] ",
              generateProcessInstance.numberOfSecondToWait, endTime - startTime,
              (result != null ? result.getProcessInstanceKey() : "null"), exception);
        }

        // ------------------ normal
      } else
        processInstanceStep3.send().join();

      // We log if the index is %100. We are in a multi thread mode, but it's still relevant
      if ((index % 100 == 0) && (index > 0)) {
        long currentDelay = System.currentTimeMillis() - generateProcessInstance.beginTimeOperation;
        if (currentDelay == 0)
          currentDelay = 1;
        logger.info("... generate {} in {} ms ( {}/s)", index, currentDelay,
            currentDelay > 1000 ? index / (currentDelay / 1000) : 0);
      }

    }
  }
}