// This code is a scheduled task manager implemented in Java.

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

class ScheduledTask {
    private String taskName;
    private String[] taskCommand;
    private String scheduleType;
    private int scheduleValue;
    private LocalDateTime lastRunTime;
    private String dataFilePath;
    private String tempDataDir = "temp";  // Temporary data directory
    private String tempDataFile;

    public ScheduledTask(String taskName, String[] taskCommand, String scheduleType, int scheduleValue, String dataFilePath) {
        this.taskName = taskName;
        this.taskCommand = taskCommand;
        this.scheduleType = scheduleType;
        this.scheduleValue = scheduleValue;
        this.dataFilePath = dataFilePath;
        this.tempDataFile = Paths.get(tempDataDir, taskName + "_temp_data.txt").toString();  // Temporary data file
    }

    private LocalDateTime loadLastRunTime() {
        try {
            String lastRunStr = new String(Files.readAllBytes(Paths.get(taskName + "_last_run.txt"))).trim();
            return LocalDateTime.parse(lastRunStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"));
        } catch (IOException e) {
            return null;
        }
    }

    private void saveLastRunTime() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(taskName + "_last_run.txt"))) {
            writer.write(lastRunTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public LocalDateTime getNextRunTime() {
        LocalDateTime now = LocalDateTime.now();
        if (lastRunTime == null) {
            lastRunTime = now;
            return now;
        }

        LocalDateTime nextRunTime;
        switch (scheduleType) {
            case "second":
                nextRunTime = lastRunTime.plusSeconds(scheduleValue);
                break;
            case "minute":
                nextRunTime = lastRunTime.plusMinutes(scheduleValue);
                break;
            case "hour":
                nextRunTime = lastRunTime.plusHours(scheduleValue);
                break;
            case "day":
                nextRunTime = lastRunTime.plusDays(scheduleValue);
                break;
            case "month":
                nextRunTime = lastRunTime.plusDays(30 * scheduleValue); // Approximate 30 days per month
                break;
            case "year":
                nextRunTime = lastRunTime.plusDays(365 * scheduleValue); // Approximate 365 days per year
                break;
            default:
                throw new IllegalArgumentException("Invalid schedule type: " + scheduleType);
        }

        if (nextRunTime.isBefore(now)) { // If the next run time is before the current time, update to the next time period
            nextRunTime = getNextRunTime();
        }

        return nextRunTime;
    }

    public void run() {
        try {
            // Create temporary data directory if it doesn't exist
            Files.createDirectories(Paths.get(tempDataDir));

            // Execute command and write standard output to temporary file
            ProcessBuilder processBuilder = new ProcessBuilder(taskCommand);
            processBuilder.redirectOutput(new File(tempDataFile));
            Process process = processBuilder.start();
            process.waitFor();

            lastRunTime = LocalDateTime.now();
            saveLastRunTime();
            writeLog("Task " + taskName + " executed successfully, current time: " + LocalDateTime.now());

            // Append the content of the temporary data file to the target file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(dataFilePath, true));
                 BufferedReader tempReader = new BufferedReader(new FileReader(tempDataFile))) {
                String line;
                while ((line = tempReader.readLine()) != null) {
                    writer.write(LocalDateTime.now() + " - Task " + taskName + " executed successfully, output: " + line);
                    writer.newLine();
                }
            }

        } catch (IOException | InterruptedException e) {
            writeLog("Task " + taskName + " execution failed: " + e.getMessage());
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(dataFilePath, true))) {
                writer.write(LocalDateTime.now() + " - Task " + taskName + " execution failed: " + e.getMessage());
                writer.newLine();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } finally {
            // Clean up temporary data file
            try {
                Files.deleteIfExists(Paths.get(tempDataFile));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void writeLog(String message) {
        try (BufferedWriter logWriter = new BufferedWriter(new FileWriter("scheduled_tasks.log", true))) {
            logWriter.write(LocalDateTime.now() + " - " + message);
            logWriter.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

public class Main {
    public static void main(String[] args) {
        // Define multiple tasks
        List<ScheduledTask> tasks = new ArrayList<>();
        tasks.add(new ScheduledTask("Task1", new String[]{"python", "task1.py"}, "minute", 2, "task_log.txt")); // Execute every 2 minutes
        tasks.add(new ScheduledTask("Task2", new String[]{"python", "task2.py"}, "hour", 6, "task_log.txt")); // Execute every 6 hours
        tasks.add(new ScheduledTask("Task3", new String[]{"python", "task3.py"}, "day", 1, "task_log.txt")); // Execute daily

        while (true) {
            // Get the next run time for each task
            List<LocalDateTime> nextRunTimes = new ArrayList<>();
            for (ScheduledTask task : tasks) {
                nextRunTimes.add(task.getNextRunTime());
            }

            // Find the task with the earliest next run time
            int minIndex = nextRunTimes.indexOf(nextRunTimes.stream().min(LocalDateTime::compareTo).orElseThrow());
            LocalDateTime nextRunTime = nextRunTimes.get(minIndex);

            // Wait until the next run time
            try {
                Thread.sleep(Duration.between(LocalDateTime.now(), nextRunTime).toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Execute the task with the earliest next run time  
            tasks.get(minIndex).run();
        }
    }
}
