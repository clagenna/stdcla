package sm.clagenna.stdcla.utils.concurrency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service that executes multiple Callable tasks and manages their Future
 * results
 */
public class CallableFutureService {
  private final ExecutorService executorService;
  private final int             maxThreads;

  public CallableFutureService(int mxThs) {
    maxThreads = mxThs;
    executorService = Executors.newFixedThreadPool(maxThreads);
  }

  /**
   * Executes a collection of Callable tasks and returns their Future results
   *
   * @param tasks
   *          Collection of Callable tasks to execute
   * @return List of Future objects representing the pending results
   */
  public <T> List<Future<T>> submitTasks(Collection<Callable<T>> tasks) {
    List<Future<T>> futures = new ArrayList<>();

    for (Callable<T> task : tasks) {
      Future<T> future = executorService.submit(task);
      futures.add(future);
    }
    return futures;
  }

  /**
   * Executes tasks and waits for all to complete, returning results in order
   *
   * @param tasks
   *          Collection of Callable tasks to execute
   * @param timeout
   *          Maximum time to wait for completion
   * @param unit
   *          Time unit for timeout
   * @return List of results in the same order as input tasks
   */
  public <T> List<T> executeAndWaitForAll(Collection<Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {

    List<Future<T>> futures = submitTasks(tasks);
    List<T> results = new ArrayList<>();

    long deadline = System.currentTimeMillis() + unit.toMillis(timeout);

    for (Future<T> future : futures) {
      long remainingTime = deadline - System.currentTimeMillis();
      if (remainingTime <= 0) {
        throw new TimeoutException("Overall timeout exceeded");
      }

      T result = future.get(remainingTime, TimeUnit.MILLISECONDS);
      results.add(result);
    }

    return results;
  }

  /**
   * Executes tasks and returns results as they complete (not in original order)
   *
   * @param tasks
   *          Collection of Callable tasks to execute
   * @param timeout
   *          Maximum time to wait for all tasks
   * @param unit
   *          Time unit for timeout
   * @return List of results as they complete
   */
  public <T> List<T> executeAndCollectAsCompleted(Collection<Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {

    /* List<Future<T>> futures = */ submitTasks(tasks);
    List<T> results = new ArrayList<>();

    CompletionService<T> completionService = new ExecutorCompletionService<>(executorService);

    // Submit all tasks to completion service
    for (Callable<T> task : tasks) {
      completionService.submit(task);
    }

    // Collect results as they complete
    long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
    for (int i = 0; i < tasks.size(); i++) {
      long remainingTime = deadline - System.currentTimeMillis();
      if (remainingTime <= 0) {
        throw new TimeoutException("Timeout while waiting for task completion");
      }

      Future<T> completedFuture = completionService.poll(remainingTime, TimeUnit.MILLISECONDS);
      if (completedFuture == null) {
        throw new TimeoutException("No task completed within timeout");
      }

      results.add(completedFuture.get());
    }

    return results;
  }

  /**
   * Executes tasks with individual timeouts and handles failures gracefully
   *
   * @param tasks
   *          Collection of Callable tasks to execute
   * @param timeoutPerTask
   *          Maximum time to wait for each individual task
   * @param unit
   *          Time unit for timeout
   * @return List of TaskResult objects containing success/failure information
   */
  public <T> List<TaskResult<T>> executeWithIndividualTimeouts(Collection<Callable<T>> tasks, long timeoutPerTask, TimeUnit unit) {

    List<Future<T>> futures = submitTasks(tasks);
    List<TaskResult<T>> results = new ArrayList<>();

    for (int i = 0; i < futures.size(); i++) {
      Future<T> future = futures.get(i);
      try {
        T result = future.get(timeoutPerTask, unit);
        results.add(TaskResult.success(i, result));
      } catch (TimeoutException e) {
        future.cancel(true); // Interrupt the task
        results.add(TaskResult.timeout(i));
      } catch (ExecutionException e) {
        results.add(TaskResult.failure(i, e.getCause()));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        results.add(TaskResult.interrupted(i));
      }
    }

    return results;
  }

  /**
   * Shutdown the service gracefully
   */
  public void shutdown() {
    executorService.shutdown();
  }

  /**
   * Force shutdown the service
   */
  public void shutdownNow() {
    executorService.shutdownNow();
  }

  /**
   * Wait for service termination
   */
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return executorService.awaitTermination(timeout, unit);
  }

  // Inner class to represent task execution results
  public static class TaskResult<T> {
    private final int          taskIndex;
    private final T            result;
    private final Throwable    exception;
    private final ResultStatus status;

    public enum ResultStatus {
      SUCCESS, FAILURE, TIMEOUT, INTERRUPTED
    }

    private TaskResult(int taskIndex, T result, Throwable exception, ResultStatus status) {
      this.taskIndex = taskIndex;
      this.result = result;
      this.exception = exception;
      this.status = status;
    }

    public static <T> TaskResult<T> success(int taskIndex, T result) {
      return new TaskResult<>(taskIndex, result, null, ResultStatus.SUCCESS);
    }

    public static <T> TaskResult<T> failure(int taskIndex, Throwable exception) {
      return new TaskResult<>(taskIndex, null, exception, ResultStatus.FAILURE);
    }

    public static <T> TaskResult<T> timeout(int taskIndex) {
      return new TaskResult<>(taskIndex, null, null, ResultStatus.TIMEOUT);
    }

    public static <T> TaskResult<T> interrupted(int taskIndex) {
      return new TaskResult<>(taskIndex, null, null, ResultStatus.INTERRUPTED);
    }

    // Getters
    public int getTaskIndex() {
      return taskIndex;
    }

    public T getResult() {
      return result;
    }

    public Throwable getException() {
      return exception;
    }

    public ResultStatus getStatus() {
      return status;
    }

    public boolean isSuccess() {
      return status == ResultStatus.SUCCESS;
    }
  }

  // Example usage and demonstration
}
