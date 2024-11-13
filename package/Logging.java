// This code is using Java logging framework and file handling
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;

public class LoggerUtility {
    private static final int PAGE_SIZE = 4096;
    private static final String TEMP_DIRECTORY = "logs";  // 存储日志文件的目录
    private static final String LOG_FILE_NAME = "logging.txt";  // 统一的日志文件名

    public static void ensureLogDirectory() {
        // 确保日志目录存在，如果不存在则创建
        File tempDir = new File(TEMP_DIRECTORY);
        if (!tempDir.exists()) {
            tempDir.mkdir();
        }
    }

    public static String tail(String filePath, int numberOfLines) {
        // 实现 tail -n
        StringBuilder result = new StringBuilder();
        try (RandomAccessFile file = new RandomAccessFile(filePath, "r")) {
            long fileLength = file.length();
            long remainingBytes = fileLength % PAGE_SIZE;
            long pageCount = fileLength / PAGE_SIZE;
            long readLength = remainingBytes != 0 ? remainingBytes : PAGE_SIZE;

            List<String> lines = new ArrayList<>();
            while (true) {
                if (readLength >= fileLength) {
                    file.seek(0);
                    lines.addAll(Arrays.asList(new String(file.readAllBytes()).split("\n")));
                    break;
                }

                file.seek(fileLength - readLength);
                lines.addAll(Arrays.asList(new String(file.readNBytes((int) readLength)).split("\n")));
                int lineCount = lines.size() - 1;  // 末行可能不完整，减一行，加大读取量

                if (lineCount >= numberOfLines) {
                    break;
                } else {
                    readLength += PAGE_SIZE;
                    pageCount--;
                }
            }

            for (int i = Math.max(0, lines.size() - numberOfLines); i < lines.size(); i++) {
                result.append(lines.get(i)).append("\n");
            }
        } catch (IOException e) {
            System.out.println("Error reading the file: " + e.getMessage());
        }
        return result.toString();
    }

    public static Logger getLogger(String name) {
        // 作用同标准模块 logging.getLogger(name)
        String logFormat = "%1$tF %1$tT - %2$s - %3$s - %4$s - line %5$d - %6$s - %7$s";
        Logger logger = Logger.getLogger(name);
        logger.setLevel(Level.INFO);

        String logFilePath = TEMP_DIRECTORY + File.separator + LOG_FILE_NAME;

        // 文件处理器，支持日志文件滚动
        if (logger.getHandlers().length == 0) {
            try {
                FileHandler fileHandler = new FileHandler(logFilePath, 10 * 1024 * 1024, 5, true); // 10 MB
                fileHandler.setFormatter(new SimpleFormatter() {
                    @Override
                    public String format(LogRecord record) {
                        return String.format(logFormat, new Date(record.getMillis()), record.getLoggerName(),
                                record.getSourceClassName(), record.getSourceMethodName(), record.getLineNumber(),
                                record.getLevel(), record.getMessage());
                    }
                });
                logger.addHandler(fileHandler);
            } catch (IOException e) {
                System.out.println("日志处理器初始化失败: " + e.getMessage());
            }
        }
        return logger;
    }

    public static String readLog(int lines) {
        // 获取最新的指定行数的 log
        String logPath = TEMP_DIRECTORY + File.separator + LOG_FILE_NAME;
        try {
            if (Files.exists(Paths.get(logPath))) {
                return tail(logPath, lines);
            }
        } catch (Exception e) {
            System.out.println("读取日志时出错: " + e.getMessage());
        }
        return "";
    }

    public static void splitLogs(String logFileName, Map<String, String> outputFiles) {
        // 自动从 logging.txt 中获取日志，并根据程序名称将日志分割到不同的文件中
        String logPath = TEMP_DIRECTORY + File.separator + LOG_FILE_NAME;

        if (!Files.exists(Paths.get(logPath))) {
            // 如果日志文件不存在，则不进行分割
            for (String filePath : outputFiles.values()) {
                try {
                    if (!Files.exists(Paths.get(filePath))) {
                        new File(filePath).createNewFile();  // 创建空文件
                    }
                } catch (IOException e) {
                    System.out.println("创建文件失败: " + e.getMessage());
                }
            }
            return;
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(logPath));
        } catch (IOException e) {
            System.out.println("读取日志文件失败: " + e.getMessage());
            return;
        }

        Map<String, List<String>> logs = new HashMap<>();
        logs.put("default", new ArrayList<>());  // 用于收集未匹配的日志

        for (String line : lines) {
            if (line.contains(" - ")) {
                String programName = line.split(" - ")[1];  // 提取程序名（即 logger 名）
                logs.putIfAbsent(programName, new ArrayList<>());
                logs.get(programName).add(line);
            }
        }

        // 将日志写入各自的文件中
        for (Map.Entry<String, List<String>> entry : logs.entrySet()) {
            String programName = entry.getKey();
            List<String> logLines = entry.getValue();

            String logFilePath = TEMP_DIRECTORY + File.separator + programName + ".txt";
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath, true))) {
                for (String logLine : logLines) {
                    writer.write(logLine);
                    writer.newLine();
                }
            } catch (IOException e) {
                System.out.println("写入日志文件失败: " + e.getMessage());
            }
        }

        // 清空 logging.txt 文件
        try {
            new PrintWriter(logPath).close();
        } catch (IOException e) {
            System.out.println("清空日志文件失败: " + e.getMessage());
        }
    }
}
