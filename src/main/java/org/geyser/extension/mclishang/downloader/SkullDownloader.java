package org.geyser.extension.mclishang.downloader;

import org.geyser.extension.mclishang.config.GeyserHeadPackConfig;
import org.geysermc.geyser.api.extension.Extension;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 头颅皮肤下载器
 */
public class SkullDownloader {
    private final Extension extension;
    private final GeyserHeadPackConfig config;
    private final File cacheDir;
    private final Set<String> currentHashes = new HashSet<>();
    private final Map<String, Boolean> downloadResults = new ConcurrentHashMap<>();
    
    private static final String TEXTURE_URL_PREFIX = "http://textures.minecraft.net/texture/";

    /**
     * 创建下载器
     * @param extension 扩展实例
     * @param config 配置
     * @param geyserCacheDir Geyser缓存目录
     */
    public SkullDownloader(Extension extension, GeyserHeadPackConfig config, File geyserCacheDir) {
        this.extension = extension;
        this.config = config;
        
        // 确保Geyser的缓存目录存在
        if (!geyserCacheDir.exists()) {
            geyserCacheDir.mkdirs();
        }
        
        // 创建player_skulls目录
        this.cacheDir = new File(geyserCacheDir, "player_skulls");
        if (!this.cacheDir.exists()) {
            this.cacheDir.mkdirs();
        }
    }

    /**
     * 下载多个头颅皮肤
     * @param hashes 皮肤哈希列表
     * @return 成功下载的数量
     */
    public int downloadSkins(List<String> hashes) {
        // 记录当前哈希以用于清理未使用的缓存
        currentHashes.clear();
        currentHashes.addAll(hashes);
        
        // 重置下载结果
        downloadResults.clear();
        
        // 创建线程池
        ExecutorService threadPool = Executors.newFixedThreadPool(config.getDownloadThreads());
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        
        extension.logger().info("开始多线程下载 " + hashes.size() + " 个头颅皮肤文件...");
        
        // 遍历所有哈希值，提交下载任务
        for (String hash : hashes) {
            if (hash == null || hash.trim().isEmpty()) {
                continue;
            }
            
            final String cleanHash = hash.trim();
            
            // 检查缓存
            if (isCached(cleanHash)) {
                if (config.isDebug()) {
                    extension.logger().info("头颅皮肤缓存已存在: " + cleanHash);
                }
                downloadResults.put(cleanHash, true);
                successCount.incrementAndGet();
                continue;
            }
            
            // 提交下载任务
            threadPool.submit(() -> {
                boolean success = downloadSkin(cleanHash);
                downloadResults.put(cleanHash, success);
                
                if (success) {
                    successCount.incrementAndGet();
                } else {
                    failCount.incrementAndGet();
                }
            });
        }
        
        // 关闭线程池并等待所有任务完成
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            extension.logger().error("下载任务被中断", e);
            Thread.currentThread().interrupt();
        }
        
        extension.logger().info("头颅皮肤下载完成 - 成功: " + successCount.get() + ", 失败: " + failCount.get());
        
        // 清理未使用的缓存文件
        if (config.isCleanUnused()) {
            cleanUnusedCache();
        }
        
        return successCount.get();
    }

    /**
     * 下载单个头颅皮肤
     * @param hash 皮肤哈希
     * @return 是否成功
     */
    private boolean downloadSkin(String hash) {
        String url = TEXTURE_URL_PREFIX + hash;
        File outFile = new File(cacheDir, hash + ".png");
        
        // 重试逻辑
        for (int attempt = 0; attempt <= config.getRetryCount(); attempt++) {
            if (attempt > 0) {
                if (config.isDebug()) {
                    extension.logger().info("重试下载 (" + attempt + "/" + config.getRetryCount() + "): " + hash);
                }
                try {
                    Thread.sleep(config.getRetryDelay());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            
            try {
                if (downloadFile(url, outFile)) {
                    if (config.isDebug()) {
                        extension.logger().info("成功下载头颅皮肤: " + hash);
                    }
                    return true;
                }
            } catch (IOException e) {
                if (config.isDebug()) {
                    extension.logger().error("下载头颅皮肤失败: " + hash, e);
                }
            }
        }
        
        extension.logger().info("下载头颅皮肤失败（已尝试 " + (config.getRetryCount() + 1) + " 次）: " + hash);
        return false;
    }

    /**
     * 下载文件
     * @param urlStr URL字符串
     * @param outputFile 输出文件
     * @return 是否成功
     * @throws IOException 发生IO异常
     */
    private boolean downloadFile(String urlStr, File outputFile) throws IOException {
        // 创建父目录
        outputFile.getParentFile().mkdirs();
        
        // 配置代理
        Proxy proxy = Proxy.NO_PROXY;
        if (config.isProxyEnabled()) {
            Proxy.Type proxyType = "SOCKS".equalsIgnoreCase(config.getProxyType()) 
                ? Proxy.Type.SOCKS 
                : Proxy.Type.HTTP;
            proxy = new Proxy(proxyType, new InetSocketAddress(config.getProxyHost(), config.getProxyPort()));
        }
        
        // 创建连接
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
        connection.setConnectTimeout(config.getConnectTimeout());
        connection.setReadTimeout(config.getReadTimeout());
        
        // 如果需要代理认证
        if (config.isProxyEnabled() && 
            !config.getProxyUsername().isEmpty() && 
            !config.getProxyPassword().isEmpty()) {
            
            String auth = config.getProxyUsername() + ":" + config.getProxyPassword();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            connection.setRequestProperty("Proxy-Authorization", "Basic " + encodedAuth);
        }
        
        // 进行连接
        connection.connect();
        
        // 检查响应码
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP错误: " + responseCode + " " + connection.getResponseMessage());
        }
        
        // 下载文件
        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(outputFile)) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        
        return true;
    }

    /**
     * 检查头颅皮肤是否已缓存
     * @param hash 皮肤哈希
     * @return 是否已缓存
     */
    public boolean isCached(String hash) {
        File cacheFile = new File(cacheDir, hash + ".png");
        return cacheFile.exists() && cacheFile.length() > 0;
    }

    /**
     * 清理未使用的缓存文件
     */
    private void cleanUnusedCache() {
        File[] files = cacheDir.listFiles((dir, name) -> name.endsWith(".png"));
        if (files == null) {
            return;
        }
        
        int removedCount = 0;
        for (File file : files) {
            String fileName = file.getName();
            String hash = fileName.substring(0, fileName.lastIndexOf('.'));
            
            if (!currentHashes.contains(hash)) {
                if (file.delete()) {
                    removedCount++;
                    if (config.isDebug()) {
                        extension.logger().info("已删除未使用的缓存文件: " + fileName);
                    }
                }
            }
        }
        
        if (removedCount > 0) {
            extension.logger().info("已清理 " + removedCount + " 个未使用的缓存文件");
        }
    }

    /**
     * 获取下载结果
     * @return 下载结果映射
     */
    public Map<String, Boolean> getDownloadResults() {
        return new ConcurrentHashMap<>(downloadResults);
    }

    /**
     * 获取缓存目录
     * @return 缓存目录
     */
    public File getCacheDir() {
        return cacheDir;
    }
} 