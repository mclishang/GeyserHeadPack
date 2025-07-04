package org.geyser.extension.mclishang.downloader;

import org.geyser.extension.mclishang.config.GeyserHeadPackConfig;
import org.geyser.extension.mclishang.util.I18n;
import org.geysermc.geyser.api.extension.Extension;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 头颅皮肤下载器 - 与Geyser处理方式保持一致
 */
public class SkullDownloader {

    private static final String TEXTURE_URL_PREFIX = "https://textures.minecraft.net/texture/";
    
    private final Extension extension;
    private final GeyserHeadPackConfig config;
    private final File cacheDir;
    private final ExecutorService downloadExecutor;
    private final Set<String> currentHashes = new HashSet<>();
    private final ConcurrentHashMap<String, Boolean> downloadResults = new ConcurrentHashMap<>();
    private final I18n i18n;

    public SkullDownloader(Extension extension, GeyserHeadPackConfig config, File geyserCacheDir, I18n i18n) {
        this.extension = extension;
        this.config = config;
        this.i18n = i18n;
        
        // 使用正确的目录结构，与Geyser保持一致
        this.cacheDir = new File(geyserCacheDir, "player_skulls");
        if (!this.cacheDir.exists()) {
            this.cacheDir.mkdirs();
        }
        
        this.downloadExecutor = Executors.newFixedThreadPool(config.getDownloadThreads());
        
        if (config.isDebug()) {
            extension.logger().info(i18n.get("debug.thread_pool_size", config.getDownloadThreads()));
        }
    }

    public int downloadSkins(List<String> hashes) {
        if (hashes == null || hashes.isEmpty()) {
            return 0;
        }
        
        currentHashes.clear();
        currentHashes.addAll(hashes);
        
        downloadResults.clear();
        
        AtomicInteger successCount = new AtomicInteger(0);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        extension.logger().info(i18n.get("message.start_download", hashes.size()));
        
        for (String hash : hashes) {
            if (hash == null || hash.trim().isEmpty()) {
                continue;
            }
            
            final String cleanHash = hash.trim();
            
            if (isCached(cleanHash) && !config.isForceDownload()) {
                successCount.incrementAndGet();
                continue;
            }
            
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    if (downloadSkin(cleanHash)) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    if (config.isDebug()) {
                        extension.logger().error(i18n.get("error.download_skin", cleanHash, e.getMessage()));
                    }
                }
            }, downloadExecutor);
            
            futures.add(future);
        }
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        shutdownExecutor();
        
        if (config.isCleanUnused()) {
            cleanUnusedCache();
        }
        
        extension.logger().info(i18n.get("message.download_finished", successCount.get(), hashes.size()));
        
        return successCount.get();
    }
    
    private void shutdownExecutor() {
        downloadExecutor.shutdown();
        try {
            if (!downloadExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                downloadExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            downloadExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    private boolean isCached(String hash) {
        File file = new File(cacheDir, hash + ".png");
        return file.exists() && file.length() > 0;
    }
    
    private boolean downloadSkin(String hash) {
        if (hash == null || hash.isEmpty()) {
            return false;
        }
        
        String cleanHash = hash.trim();
        
        File skinFile = new File(cacheDir, cleanHash + ".png");
        if (skinFile.exists() && skinFile.length() > 0 && !config.isForceDownload()) {
            return true;
        }
        
        // 添加重试机制
        int maxRetries = 3;
        int retryDelay = 1000; // 毫秒
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                String urlStr = TEXTURE_URL_PREFIX + cleanHash;
                URL url = new URL(urlStr);
                
                HttpURLConnection connection = createConnection(url);
                
                int responseCode = connection.getResponseCode();
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (InputStream inputStream = connection.getInputStream()) {
                        BufferedImage originalImage = ImageIO.read(inputStream);
                        
                        if (originalImage != null) {
                            // 使用与Geyser完全相同的处理方法
                            BufferedImage processedImage = processSkullImage(originalImage);
                            
                            // 确保目录存在
                            skinFile.getParentFile().mkdirs();
                            
                            ImageIO.write(processedImage, "PNG", skinFile);
                            
                            if (config.isDebug()) {
                                extension.logger().info(i18n.get("debug.download_success", cleanHash, skinFile.getAbsolutePath()));
                            }
                            
                            return true;
                        } else {
                            if (config.isDebug()) {
                                extension.logger().error(i18n.get("error.image_read", cleanHash));
                            }
                        }
                    }
                } else {
                    if (config.isDebug()) {
                        extension.logger().error(i18n.get("error.download_failed", responseCode, cleanHash));
                    }
                    
                    // 如果是服务器错误(5xx)或服务不可用，则重试
                    if (responseCode >= 500 && responseCode < 600) {
                        if (attempt < maxRetries - 1) {
                            try {
                                Thread.sleep(retryDelay * (attempt + 1));
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                            continue;
                        }
                    }
                }
            } catch (Exception e) {
                if (config.isDebug()) {
                    extension.logger().error(i18n.get("error.download_skin", cleanHash, e.getMessage()));
                }
                
                // 对网络错误进行重试
                if (attempt < maxRetries - 1) {
                    try {
                        Thread.sleep(retryDelay * (attempt + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }
            }
            
            // 如果到达这里，说明这次尝试失败但不需要重试，直接退出循环
            break;
        }
        
        return false;
    }
    
    private HttpURLConnection createConnection(URL url) throws IOException {
        HttpURLConnection connection;
        
        if (config.isProxyEnabled()) {
            Proxy proxy;
            
            if ("SOCKS".equalsIgnoreCase(config.getProxyType())) {
                proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(config.getProxyHost(), config.getProxyPort()));
            } else {
                proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(config.getProxyHost(), config.getProxyPort()));
            }
            
            connection = (HttpURLConnection) url.openConnection(proxy);
        } else {
            connection = (HttpURLConnection) url.openConnection();
        }
        
        connection.setRequestProperty("User-Agent", "GeyserHeadPack/1.0");
        connection.setConnectTimeout(config.getConnectTimeout());
        connection.setReadTimeout(config.getReadTimeout());
        
        return connection;
    }
    
    private BufferedImage processSkullImage(BufferedImage originalImage) {
        // 与Geyser完全相同的处理方法
        // Resize skins to 48x16 to save on space and memory
        BufferedImage skullTexture = new BufferedImage(48, 16, originalImage.getType());
        // Reorder skin parts to fit into the space
        // Right, Front, Left, Back, Top, Bottom - head
        // Right, Front, Left, Back, Top, Bottom - hat
        Graphics g = skullTexture.createGraphics();
        try {
            // Right, Front, Left, Back of the head
            g.drawImage(originalImage, 0, 0, 32, 8, 0, 8, 32, 16, null);
            // Right, Front, Left, Back of the hat
            g.drawImage(originalImage, 0, 8, 32, 16, 32, 8, 64, 16, null);
            // Top and bottom of the head
            g.drawImage(originalImage, 32, 0, 48, 8, 8, 0, 24, 8, null);
            // Top and bottom of the hat
            g.drawImage(originalImage, 32, 8, 48, 16, 40, 0, 56, 8, null);
        } finally {
            g.dispose();
        }
        originalImage.flush();
        
        return skullTexture;
    }
    
    private void cleanUnusedCache() {
        if (cacheDir == null || !cacheDir.exists() || !cacheDir.isDirectory()) {
            return;
        }
        
        File[] files = cacheDir.listFiles((dir, name) -> name.endsWith(".png"));
        if (files == null) {
            return;
        }
        
        int cleanedCount = 0;
        
        for (File file : files) {
            String fileName = file.getName();
            String hash = fileName.substring(0, fileName.lastIndexOf('.'));
            
            if (!currentHashes.contains(hash)) {
                try {
                    Files.delete(file.toPath());
                    cleanedCount++;
                    
                    if (config.isDebug()) {
                        extension.logger().info(i18n.get("debug.cache_deleted", fileName));
                    }
                } catch (IOException e) {
                    if (config.isDebug()) {
                        extension.logger().error("Failed to delete cache file: " + fileName);
                    }
                }
            }
        }
        
        if (cleanedCount > 0) {
            extension.logger().info(i18n.get("message.cleaned_cache", cleanedCount));
        }
    }
}