package org.xlyo.cocomonyab.config.mongo;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.config.data.DataDirectoryManager;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 嵌入式 MongoDB 配置类
 * 当 spring.data.mongodb.mode=embedded 时启用
 * 通过下载 MongoDB 压缩包并启动 mongod 进程实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.mongodb.mode", havingValue = "embedded", matchIfMissing = true)
public class EmbeddedMongoConfiguration {
    
    private static final String MONGODB_DOWNLOAD_URL_TEMPLATE = 
        "https://fastdl.mongodb.org/windows/mongodb-windows-x86_64-%s.zip";
    
    private final MongoDBProperties properties;
    private final DataDirectoryManager dataDirectoryManager;
    private Process mongodProcess;
    private volatile boolean isShuttingDown = false;

    @PostConstruct
    public void startEmbeddedMongo() {
        try {
            String version = properties.getEmbedded().getVersion();
            int port = properties.getEmbedded().getPort();
            String bindIp = properties.getEmbedded().getBindIp();
            
            // 使用 DataDirectoryManager 获取存储目录
            String storageDirectory = dataDirectoryManager.getMongoDbPath().toString();
            Path storagePath = Paths.get(storageDirectory);
            
            // 创建存储目录（如果不存在）
            if (!Files.exists(storagePath)) {
                log.info("存储目录不存在，正在创建: {}", storageDirectory);
                Files.createDirectories(storagePath);
            }
            
            // 验证目录是否可写
            if (!Files.isWritable(storagePath)) {
                throw new IllegalStateException(
                    String.format("存储目录不可写: %s", storageDirectory)
                );
            }
            
            // 确保 MongoDB 二进制文件存在
            Path mongodPath = ensureMongoDBBinary(version);
            
            log.info("正在启动嵌入式 MongoDB, 版本: {}, 存储目录: {}, 端口: {}, 绑定IP: {}", 
                version, storageDirectory, port, bindIp);
            
            // 启动 mongod 进程
            ProcessBuilder processBuilder = new ProcessBuilder(
                mongodPath.toString(),
                "--dbpath", storagePath.toAbsolutePath().toString(),
                "--port", String.valueOf(port),
                "--bind_ip", bindIp
            );
            
            processBuilder.redirectErrorStream(true);
            mongodProcess = processBuilder.start();
            
            // 添加 JVM 关闭钩子，确保进程被清理
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (!isShuttingDown) {
                    stopEmbeddedMongo();
                }
            }));
            
            // 启动日志读取线程
            startLogReader();
            
            // 等待 MongoDB 启动
            waitForMongoDBStart(bindIp, port);
            
            log.info("嵌入式 MongoDB 启动成功, 端口: {}", port);
            
        } catch (IOException e) {
            log.error("无法创建存储目录或启动 MongoDB", e);
            throw new IllegalStateException("无法创建存储目录或启动 MongoDB: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("嵌入式 MongoDB 启动失败", e);
            throw new IllegalStateException("嵌入式 MongoDB 启动失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 确保 MongoDB 二进制文件存在，如果不存在则下载
     */
    private Path ensureMongoDBBinary(String version) throws IOException {
        Path mongodbBinDir = dataDirectoryManager.getMongoBinPath();
        Path mongodExe = mongodbBinDir.resolve("bin").resolve("mongod.exe");
        
        if (Files.exists(mongodExe)) {
            log.info("MongoDB 二进制文件已存在: {}", mongodExe);
            return mongodExe;
        }
        
        log.info("MongoDB 二进制文件不存在，开始下载版本: {}", version);
        downloadAndExtractMongoDB(mongodbBinDir, version);
        
        if (!Files.exists(mongodExe)) {
            throw new IllegalStateException("MongoDB 下载失败，未找到 mongod.exe");
        }
        
        log.info("MongoDB 下载完成");
        return mongodExe;
    }
    
    /**
     * 下载并解压 MongoDB
     */
    private void downloadAndExtractMongoDB(Path targetDir, String version) throws IOException {
        Files.createDirectories(targetDir);
        Path tmpDir = dataDirectoryManager.getTmpPath();
        Files.createDirectories(tmpDir);
        
        Path zipFile = tmpDir.resolve("mongodb-" + version + ".zip");
        String downloadUrl = String.format(MONGODB_DOWNLOAD_URL_TEMPLATE, version);
        
        try {
            // 下载 MongoDB 压缩包
            log.info("正在从 {} 下载 MongoDB...", downloadUrl);
            downloadFileWithProgress(downloadUrl, zipFile);
            log.info("MongoDB 下载完成，保存至: {}", zipFile);
            
            // 解压 MongoDB
            log.info("正在解压 MongoDB...");
            extractZipFileWithProgress(zipFile, targetDir);
            log.info("MongoDB 解压完成");
            
        } catch (Exception e) {
            log.error("下载或解压 MongoDB 失败", e);
            throw e;
        } finally {
            // 删除压缩包
            if (Files.exists(zipFile)) {
                try {
                    Files.delete(zipFile);
                    log.info("已删除临时压缩包: {}", zipFile);
                } catch (IOException e) {
                    log.warn("删除临时压缩包失败: {}", zipFile, e);
                }
            }
        }
    }
    
    /**
     * 下载文件并显示进度条
     */
    private void downloadFileWithProgress(String urlString, Path targetFile) throws IOException {
        URL url = URI.create(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        
        long fileSize = connection.getContentLengthLong();
        
        try (InputStream in = connection.getInputStream();
             OutputStream out = Files.newOutputStream(targetFile)) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytesRead = 0;
            long lastProgressUpdate = 0;
            
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                
                // 每下载 1MB 更新一次进度
                if (totalBytesRead - lastProgressUpdate >= 1024 * 1024) {
                    printProgress("下载", totalBytesRead, fileSize);
                    lastProgressUpdate = totalBytesRead;
                }
            }
            
            // 确保显示 100% 进度
            printProgress("下载", totalBytesRead, fileSize);
            System.out.println(); // 换行
            
            log.info("下载完成，总大小: {} MB", totalBytesRead / (1024 * 1024));
        }
    }
    
    /**
     * 解压 ZIP 文件并显示进度
     */
    private void extractZipFileWithProgress(Path zipFile, Path targetDir) throws IOException {
        // 先计算总条目数
        int totalEntries = 0;
        try (ZipInputStream countZip = new ZipInputStream(Files.newInputStream(zipFile))) {
            while (countZip.getNextEntry() != null) {
                totalEntries++;
                countZip.closeEntry();
            }
        }
        
        try (ZipInputStream zipIn = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            int processedEntries = 0;
            
            while ((entry = zipIn.getNextEntry()) != null) {
                String entryName = entry.getName();
                
                // 只解压 bin 目录下的文件
                if (entryName.contains("/bin/")) {
                    // 移除顶层目录名
                    String relativePath = entryName.substring(entryName.indexOf("/") + 1);
                    Path filePath = targetDir.resolve(relativePath);
                    
                    if (entry.isDirectory()) {
                        Files.createDirectories(filePath);
                    } else {
                        Files.createDirectories(filePath.getParent());
                        Files.copy(zipIn, filePath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                
                zipIn.closeEntry();
                processedEntries++;
                
                // 更新进度
                if (processedEntries % 10 == 0 || processedEntries == totalEntries) {
                    printProgress("解压", processedEntries, totalEntries);
                }
            }
            
            System.out.println(); // 换行
        }
    }
    
    /**
     * 打印进度条（仅在控制台显示，不记录到日志）
     */
    private void printProgress(String operation, long current, long total) {
        if (total <= 0) {
            return;
        }
        
        int percentage = (int) ((current * 100) / total);
        int barLength = 50;
        int filledLength = (int) ((current * barLength) / total);
        
        StringBuilder bar = new StringBuilder();
        bar.append("\r").append(operation).append(": [");
        
        for (int i = 0; i < barLength; i++) {
            if (i < filledLength) {
                bar.append("=");
            } else if (i == filledLength) {
                bar.append(">");
            } else {
                bar.append(" ");
            }
        }
        
        bar.append("] ").append(percentage).append("%");
        
        if (total > 1024 * 1024) {
            bar.append(String.format(" (%.2f/%.2f MB)", 
                current / (1024.0 * 1024.0), 
                total / (1024.0 * 1024.0)));
        } else {
            bar.append(String.format(" (%d/%d)", current, total));
        }
        
        System.out.print(bar);
    }
    
    /**
     * 启动日志读取线程
     */
    private void startLogReader() {
        Thread logThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(mongodProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("MongoDB: {}", line);
                }
            } catch (IOException e) {
                if (!isShuttingDown) {
                    log.error("读取 MongoDB 日志失败", e);
                }
            }
        });
        logThread.setDaemon(true);
        logThread.setName("MongoDB-Log-Reader");
        logThread.start();
    }
    
    /**
     * 等待 MongoDB 启动完成
     * 通过尝试连接 MongoDB 端口来检测是否启动成功
     */
    private void waitForMongoDBStart(String host, int port) throws InterruptedException {
        int maxRetries = 30;
        int retryCount = 0;
        long startTime = System.currentTimeMillis();
        
        log.info("等待 MongoDB 启动...");
        
        while (retryCount < maxRetries) {
            // 检查进程是否还在运行
            if (mongodProcess != null && !mongodProcess.isAlive()) {
                throw new IllegalStateException("MongoDB 进程意外终止");
            }
            
            // 尝试连接 MongoDB 端口
            try (Socket socket = new Socket()) {
                socket.connect(new java.net.InetSocketAddress(host, port), 1000);
                long elapsedTime = System.currentTimeMillis() - startTime;
                log.info("MongoDB 已就绪，耗时: {} 毫秒", elapsedTime);
                return;
            } catch (IOException e) {
                // 连接失败，继续等待
                retryCount++;
                Thread.sleep(1000);
            }
        }
        
        throw new IllegalStateException(
            String.format("MongoDB 启动超时，在 %d 秒内未能连接到 %s:%d", maxRetries, host, port)
        );
    }
    
    @PreDestroy
    public void stopEmbeddedMongo() {
        isShuttingDown = true;
        
        if (mongodProcess != null && mongodProcess.isAlive()) {
            try {
                log.info("正在停止嵌入式 MongoDB...");
                
                // 优雅关闭：先发送 SIGTERM
                mongodProcess.destroy();
                
                // 等待进程结束
                boolean exited = mongodProcess.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
                
                if (!exited) {
                    log.warn("MongoDB 进程未在规定时间内结束，强制终止");
                    mongodProcess.destroyForcibly();
                    mongodProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                }
                
                log.info("嵌入式 MongoDB 已停止");
            } catch (InterruptedException e) {
                log.error("等待 MongoDB 进程结束时被中断", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("停止嵌入式 MongoDB 时发生错误", e);
            } finally {
                mongodProcess = null;
            }
        }
    }
}
