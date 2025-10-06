package prova.concurrency;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import sm.clagenna.stdcla.utils.concurrency.CallableFutureService;
import sm.clagenna.stdcla.utils.concurrency.CallableFutureService.TaskResult;


public class ProvaCallableFuture {
  public ProvaCallableFuture() {
    // 
  }
  
  public static void main(String[] args) {
    CallableFutureService service = new CallableFutureService(5);

    try {
      // Create sample Callable tasks that return different types
      List<Callable<String>> stringTasks = Arrays.asList(() -> {
        Thread.sleep(1000);
        return "Task 1 completed";
      }, () -> {
        Thread.sleep(500);
        return "Task 2 completed";
      }, () -> {
        Thread.sleep(1500);
        return "Task 3 completed";
      });

      System.out.println("=== Example 1: Submit and manually handle futures ===");
      List<Future<String>> futures = service.submitTasks(stringTasks);
      for (int i = 0; i < futures.size(); i++) {
        try {
          String result = futures.get(i).get(2, TimeUnit.SECONDS);
          System.out.println("Future " + i + ": " + result);
        } catch (Exception e) {
          System.out.println("Future " + i + " failed: " + e.getMessage());
        }
      }

      System.out.println("\n=== Example 2: Execute and wait for all ===");
      List<String> results = service.executeAndWaitForAll(stringTasks, 3, TimeUnit.SECONDS);
      for (int i = 0; i < results.size(); i++) {
        System.out.println("Result " + i + ": " + results.get(i));
      }

      System.out.println("\n=== Example 3: Execute with individual timeouts ===");
      List<TaskResult<String>> taskResults = service.executeWithIndividualTimeouts(stringTasks, 800, TimeUnit.MILLISECONDS);

      for (TaskResult<String> taskResult : taskResults) {
        System.out.printf("Task %d: Status=%s", taskResult.getTaskIndex(), taskResult.getStatus());
        if (taskResult.isSuccess()) {
          System.out.printf(", Result=%s", taskResult.getResult());
        }
        System.out.println();
      }

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      service.shutdown();
      try {
        if ( !service.awaitTermination(2, TimeUnit.SECONDS)) {
          service.shutdownNow();
        }
      } catch (InterruptedException e) {
        service.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }


}
