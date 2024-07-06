import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TaskProcessor {

    private static final Logger LOGGER = Logger.getLogger(TaskProcessor.class.getName());

    // 模拟处理任务的函数
    public static String doSomething(String task) {
        return task.toUpperCase();
    }

    // 获取大任务列表
    public static List<String> getTasks() {
        return Arrays.asList("task1", "task2", "task3", "task4", "task5", "task6", "task7", "task8", "task9", "task10");
    }

    // 定义任务处理函数
    public static void processTask(String task, BlockingQueue<String> resultQueue) {
        long startTime = System.currentTimeMillis();
        try {
            String taskResult = doSomething(task);
            resultQueue.put(taskResult);
            LOGGER.log(Level.INFO, "任务 {0} 处理成功.", task);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "处理任务 {0} 时发生错误: {1}", new Object[] {task, e.getMessage()});
        } finally {
            long endTime = System.currentTimeMillis();
            LOGGER.log(Level.INFO, "任务 {0} 完成，耗时 {1} 毫秒.", new Object[] {task, (endTime - startTime)});
        }
    }

    // 工作线程函数
    public static void worker(List<String> subtasks, BlockingQueue<String> resultQueue) {
        for (String task : subtasks) {
            processTask(task, resultQueue);
        }
    }

    // 将大任务分解成多个小任务
    public static List<List<String>> divideTasks(List<String> tasks, int numThreads) {
        int batchSize = tasks.size() / numThreads;
        List<List<String>> subtasks = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i += numThreads) {
            subtasks.add(tasks.subList(i, Math.min(i + numThreads, tasks.size())));
        }
        return subtasks;
    }

    // 定义任务分发函数
    public static List<String> dispatchTasks(List<String> tasks, int numThreads) {
        if (tasks.size() < 10) {  // 小任务阈值
            return dispatchTasksSmall(tasks, numThreads);
        } else {
            int adjustedNumThreads = adjustNumThreads(tasks.size());  // 动态调整线程数
            return dispatchTasksLarge(tasks, adjustedNumThreads);
        }
    }

    // 动态调整任务大小的函数
    public static int adjustNumThreads(int numTasks) {
        // 获取系统负载信息 (示例：假设系统负载低于 1.0 时，可以处理更多任务)
        // 实际应用中需要使用更准确的系统负载信息
        double systemLoad = 0.5; // 假设系统负载为 0.5
        LOGGER.log(Level.INFO, "系统负载: {0}, 任务数量: {1}", new Object[] {systemLoad, numTasks});
        if (systemLoad < 1.0 && numTasks > 10) {
            return 2;  // 适当增加线程数以处理更多任务
        } else {
            return 1;  // 否则维持原样
        }
    }

    public static List<String> dispatchTasksSmall(List<String> tasks, int numThreads) {
        // 为小任务使用单独的线程
        List<String> results = new ArrayList<>();
        for (String task : tasks) {
            BlockingQueue<String> resultQueue = new LinkedBlockingQueue<>();
            ExecutorService executor = Executors.newSingleThreadExecutor(); // 使用单线程执行器
            executor.execute(() -> processTask(task, resultQueue));
            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "线程池关闭被打断.");
            }
            results.add(resultQueue.take()); // 获取结果
        }
        return results;
    }

    public static List<String> dispatchTasksLarge(List<String> tasks, int numThreads) {
        List<String> results = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        BlockingQueue<String> resultQueue = new LinkedBlockingQueue<>();
        List<List<String>> subtasks = divideTasks(tasks, numThreads);
        List<Future<Void>> futures = new ArrayList<>();
        for (List<String> subtask : subtasks) {
            futures.add(executor.submit(() -> worker(subtask, resultQueue)));
        }
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "线程池关闭被打断.");
        }
        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.log(Level.WARNING, "任务执行失败: " + e.getMessage());
            }
        }
        while (!resultQueue.isEmpty()) {
            results.add(resultQueue.take());
        }
        return results;
    }

    public static String retryTask(String task, int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            LOGGER.log(Level.INFO, "第 {0} 次尝试处理任务: {1}", new Object[] {i + 1, task});
            try {
                BlockingQueue<String> resultQueue = new LinkedBlockingQueue<>();
                processTask(task, resultQueue);
                return resultQueue.take();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "处理任务 {0} 时发生错误: {1}", new Object[] {task, e.getMessage()});
                try {
                    Thread.sleep(1000); // 等待 1 秒后重试
                } catch (InterruptedException interruptedException) {
                    LOGGER.log(Level.WARNING, "任务重试被打断: " + interruptedException.getMessage());
                }
            }
        }
        LOGGER.log(Level.SEVERE, "任务 {0} 在重试 {1} 次后仍然失败", new Object[] {task, maxRetries});
        return null; // 重试次数最多后任务失败
    }

    public static List<String> dispatchTasksWithRetry(List<String> tasks, int numThreads) {
        LOGGER.log(Level.INFO, "分发任务并重试: {0}", tasks);
        List<String> results = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<String>> futures = new ArrayList<>();
        for (String task : tasks) {
            futures.add(executor.submit(() -> retryTask(task, 3))); // 使用 3 次重试
        }
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "线程池关闭被打断.");
        }
        for (Future<String> future : futures) {
            try {
                String result = future.get();
                if (result != null) {
                    results.add(result);
                }
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.log(Level.WARNING, "任务执行失败: " + e.getMessage());
            }
        }
        return results;
    }

    // 主函数
    public static void main(String[] args) {
        LOGGER.log(Level.INFO, "开始任务处理");
        // 定义大任务
        List<String> tasks = getTasks();

        // 定义线程数，根据CPU核心数动态设置
        int numThreads = Math.min(tasks.size(), Runtime.getRuntime().availableProcessors()); 

        // 分发任务并等待结果
        List<String> result = dispatchTasks(tasks, numThreads);
        LOGGER.log(Level.INFO, "任务处理完成，结果: {0}", result);
        System.out.println(result);
    }
}
