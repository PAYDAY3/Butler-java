// This code translates a Python script for deleting temporary files into Java.
// It includes logging and backup functionality similar to the original Python code.

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TempFileCleaner {
    private static final Logger logger = Logger.getLogger(TempFileCleaner.class.getName());
    private static Date lastExecutionTime;

    public static void deleteTempFiles(String directoryPath, String logFilePath, int days, String backupDirectoryPath, boolean dryRun) {
        if (directoryPath == null || directoryPath.isEmpty()) {
            directoryPath = "./temp";
        }
        if (logFilePath == null) {
            logFilePath = "delete_report.txt";
        }

        File directory = new File(directoryPath);
        if (!directory.exists()) {
            System.out.println("Directory " + directoryPath + " does not exist");
            logger.info("Directory " + directoryPath + " does not exist");
            return;
        }

        Date now = new Date();
        StringBuilder report = new StringBuilder();

        // Traverse the directory and its subdirectories
        for (File file : directory.listFiles()) {
            if (file.isFile()) {
                handleFile(file, now, days, backupDirectoryPath, dryRun, report);
            } else if (file.isDirectory()) {
                handleDirectory(file, now, days, backupDirectoryPath, dryRun, report);
            }
        }

        System.out.println("Files and directories meeting the criteria in the 'temp' directory have been deleted");
        logger.info("Files and directories meeting the criteria in the 'temp' directory have been deleted");
        
        if (report.length() > 0) {
            try (FileWriter fileWriter = new FileWriter(logFilePath)) {
                fileWriter.write(report.toString());
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to write delete report: " + e.getMessage(), e);
            }
        }

        // Log the current execution time
        lastExecutionTime = now;
        System.out.println("Last execution time: " + lastExecutionTime);
        logger.info("Last execution time: " + lastExecutionTime);
    }

    private static void handleFile(File file, Date now, int days, String backupDirectoryPath, boolean dryRun, StringBuilder report) {
        if (!file.canRead()) {
            logger.warning("Skipping unreadable file: " + file.getAbsolutePath());
            return;
        }

        long fileAgeInDays = (now.getTime() - file.lastModified()) / (1000 * 60 * 60 * 24);

        if (fileAgeInDays > days) {
            if (backupDirectoryPath != null && !backupDirectoryPath.isEmpty()) {
                backupFile(file, backupDirectoryPath);
            }

            if (!dryRun) {
                if (file.delete()) {
                    System.out.println("Deleted file: " + file.getAbsolutePath());
                    logger.info("Deleted file: " + file.getAbsolutePath());
                } else {
                    System.out.println("Failed to delete file " + file.getAbsolutePath());
                    logger.severe("Failed to delete file " + file.getAbsolutePath());
                }
            }
        }
    }

    private static void handleDirectory(File directory, Date now, int days, String backupDirectoryPath, boolean dryRun, StringBuilder report) {
        if (!directory.canRead()) {
            logger.warning("Skipping unreadable directory: " + directory.getAbsolutePath());
            return;
        }

        long dirAgeInDays = (now.getTime() - directory.lastModified()) / (1000 * 60 * 60 * 24);

        if (dirAgeInDays > days) {
            if (backupDirectoryPath != null && !backupDirectoryPath.isEmpty()) {
                backupDirectory(directory, backupDirectoryPath);
            }

            if (!dryRun) {
                try {
                    deleteDirectory(directory);
                    System.out.println("Deleted directory: " + directory.getAbsolutePath());
                    logger.info("Deleted directory: " + directory.getAbsolutePath());
                    report.append("Deleted directory: ").append(directory.getAbsolutePath()).append("\n");
                } catch (IOException e) {
                    System.out.println("Failed to delete directory " + directory.getAbsolutePath() + ": " + e.getMessage());
                    logger.severe("Failed to delete directory " + directory.getAbsolutePath() + ": " + e.getMessage());
                }
            }
        }
    }

    private static void backupFile(File file, String backupDirectoryPath) {
        try {
            Path backupPath = Paths.get(backupDirectoryPath, file.getName());
            Files.createDirectories(backupPath.getParent());
            Files.copy(file.toPath(), backupPath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Backed up file: " + file.getAbsolutePath() + " to " + backupPath);
        } catch (IOException e) {
            logger.severe("Failed to back up file " + file.getAbsolutePath() + ": " + e.getMessage());
        }
    }

    private static void backupDirectory(File directory, String backupDirectoryPath) {
        try {
            Path backupPath = Paths.get(backupDirectoryPath, directory.getName());
            Files.createDirectories(backupPath);
            for (File file : directory.listFiles()) {
                if (file.isFile()) {
                    backupFile(file, backupDirectoryPath);
                }
            }
            logger.info("Backed up directory: " + directory.getAbsolutePath() + " to " + backupPath);
        } catch (IOException e) {
            logger.severe("Failed to back up directory " + directory.getAbsolutePath() + ": " + e.getMessage());
        }
    }

    private static void deleteDirectory(File directory) throws IOException {
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                deleteDirectory(file);
            }
            if (!file.delete()) {
                throw new IOException("Failed to delete file: " + file.getAbsolutePath());
            }
        }
        if (!directory.delete()) {
            throw new IOException("Failed to delete directory: " + directory.getAbsolutePath());
        }
    }
}
