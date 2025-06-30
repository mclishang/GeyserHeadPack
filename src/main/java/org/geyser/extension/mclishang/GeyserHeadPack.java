package org.geyser.extension.mclishang;

import org.geyser.extension.mclishang.config.GeyserHeadPackConfig;
import org.geyser.extension.mclishang.downloader.SkullDownloader;
import org.geyser.extension.mclishang.util.GeyserCacheUtil;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomSkullsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomSkullsEvent.SkullTextureType;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserShutdownEvent;
import org.geysermc.geyser.api.extension.Extension;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * GeyserHeadPack扩展 - 用于加载和注册自定义头颅
 * 
 * @author mclishang
 * @version 1.0.0
 * @since 2025-07-01
 */
public class GeyserHeadPack implements Extension {

    private File headsConfigFile;
    private File configFile;
    private final List<String> skinHashes = new ArrayList<>();
    private GeyserHeadPackConfig config;
    private SkullDownloader downloader;
    private boolean initialized = false;
    private File geyserCacheDir;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    // 自定义日志样式
    private static final String LOG_PREFIX = "[GHP] ";
    private static final String LOG_SEPARATOR = "========================================";
    
    /**
     * 显示自定义格式的日志
     * @param message 日志消息
     */
    private void logMessage(String message) {
        this.logger().info(LOG_PREFIX + message);
    }
    
    /**
     * 显示带有分隔符的日志标题
     * @param title 标题文本
     */
    private void logTitle(String title) {
        this.logger().info("");
        this.logger().info(LOG_SEPARATOR);
        this.logger().info(LOG_PREFIX + title);
        this.logger().info(LOG_SEPARATOR);
        this.logger().info("");
    }
    
    /**
     * 显示带有统计信息的日志
     * @param message 消息前缀
     * @param count 计数
     * @param total 总数
     */
    private void logStats(String message, int count, int total) {
        this.logger().info(LOG_PREFIX + message + ": " + count + "/" + total + " (" + (total > 0 ? (count * 100 / total) : 0) + "%)");
    }

    /**
     * 确保配置文件路径已初始化
     */
    private void ensureConfigFilesInitialized() {
        if (headsConfigFile == null || configFile == null) {
            Path dataFolder = this.dataFolder();
            if (!dataFolder.toFile().exists()) {
                dataFolder.toFile().mkdirs();
            }
            
            // 初始化头颅配置文件
            headsConfigFile = dataFolder.resolve("heads.yml").toFile();
            
            // 初始化主配置文件 - 使用不同的名称避免与Geyser冲突
            configFile = dataFolder.resolve("headpack.yml").toFile();
        }
    }

    /**
     * 尝试通过反射手动注册头颅
     */
    private void registerHeadsManually() {
        try {
            // 获取GeyserImpl实例
            Class<?> geyserClass = Class.forName("org.geysermc.geyser.GeyserImpl");
            Field instanceField = geyserClass.getDeclaredField("instance");
            instanceField.setAccessible(true);
            Object geyserImpl = instanceField.get(null);
            
            if (geyserImpl != null) {
                // 尝试获取CustomSkullManager
                Method getCustomSkullManagerMethod = null;
                
                // 尝试不同的方法名
                String[] possibleMethodNames = {
                    "getCustomSkullManager", 
                    "getSkullManager", 
                    "getCustomSkulls",
                    "getSkullRegistry"
                };
                
                for (String methodName : possibleMethodNames) {
                    try {
                        getCustomSkullManagerMethod = geyserClass.getMethod(methodName);
                        break;
                    } catch (NoSuchMethodException e) {
                        // 继续尝试下一个方法名
                    }
                }
                
                if (getCustomSkullManagerMethod != null) {
                    Object skullManager = getCustomSkullManagerMethod.invoke(geyserImpl);
                    
                    if (skullManager != null) {
                        // 尝试获取注册方法
                        Method registerMethod = null;
                        
                        String[] possibleRegisterMethods = {
                            "register",
                            "registerSkin",
                            "registerHash",
                            "addSkull",
                            "addCustomSkull"
                        };
                        
                        for (String methodName : possibleRegisterMethods) {
                            try {
                                // 尝试不同参数类型的方法
                                for (Method method : skullManager.getClass().getMethods()) {
                                    if (method.getName().equals(methodName) && method.getParameterCount() > 0) {
                                        registerMethod = method;
                                        break;
                                    }
                                }
                                
                                if (registerMethod != null) {
                                    break;
                                }
                            } catch (Exception e) {
                                // 继续尝试下一个方法名
                            }
                        }
                        
                        if (registerMethod != null) {
                            logMessage("找到注册方法: " + registerMethod.getName() + ", 参数数量: " + registerMethod.getParameterCount());
                            
                            // 根据参数数量调用不同的注册方法
                            int successCount = 0;
                            
                            for (String hash : skinHashes) {
                                try {
                                    if (registerMethod.getParameterCount() == 1) {
                                        registerMethod.invoke(skullManager, hash);
                                    } else if (registerMethod.getParameterCount() == 2) {
                                        // 尝试使用字符串和枚举类型
                                        // 查找可能的枚举类型
                                        Class<?> enumType = registerMethod.getParameterTypes()[1];
                                        if (enumType.isEnum()) {
                                            Object[] enumConstants = enumType.getEnumConstants();
                                            if (enumConstants.length > 0) {
                                                // 尝试找到名为SKIN_HASH的枚举值
                                                Object enumValue = null;
                                                for (Object constant : enumConstants) {
                                                    if (constant.toString().equals("SKIN_HASH")) {
                                                        enumValue = constant;
                                                        break;
                                                    }
                                                }
                                                
                                                // 如果没找到，使用第一个枚举值
                                                if (enumValue == null) {
                                                    enumValue = enumConstants[0];
                                                }
                                                
                                                registerMethod.invoke(skullManager, hash, enumValue);
                                            }
                                        } else {
                                            // 如果第二个参数不是枚举，尝试使用布尔值
                                            registerMethod.invoke(skullManager, hash, true);
                                        }
                                    }
                                    
                                    successCount++;
                                    if (successCount % 500 == 0) {
                                        logStats("已手动注册头颅", successCount, skinHashes.size());
                                    }
                                } catch (Exception e) {
                                    this.logger().error(LOG_PREFIX + "手动注册头颅失败: " + hash, e);
                                }
                            }
                            
                            logTitle("手动注册完成，成功注册 " + successCount + " 个头颅");
                        } else {
                            this.logger().error(LOG_PREFIX + "无法找到注册头颅的方法");
                        }
                    } else {
                        this.logger().error(LOG_PREFIX + "无法获取CustomSkullManager");
                    }
                } else {
                    this.logger().error(LOG_PREFIX + "无法找到获取CustomSkullManager的方法");
                }
            } else {
                this.logger().error(LOG_PREFIX + "无法获取GeyserImpl实例");
            }
        } catch (Exception e) {
            this.logger().error(LOG_PREFIX + "手动注册头颅时出错", e);
        }
    }

    @Subscribe
    public void onGeyserDefineCustomSkulls(GeyserDefineCustomSkullsEvent event) {
        logMessage("GeyserDefineCustomSkullsEvent 触发，准备注册头颅");
        
        // 如果还没有初始化，尝试先初始化
        if (!initialized) {
            logMessage("插件尚未完成初始化，尝试初始化...");
            
            // 初始化配置文件路径
            ensureConfigFilesInitialized();
            
            // 确保所有配置文件都存在
            initializeConfigFiles();
            
            // 加载配置文件
            loadConfiguration();
            
            // 加载头颅配置
            loadHeadsConfig();
            
            // 获取Geyser缓存目录
            try {
                geyserCacheDir = GeyserCacheUtil.getGeyserCacheDir(this);
            } catch (Exception e) {
                this.logger().error(LOG_PREFIX + "获取Geyser缓存目录失败", e);
            }
        }
        
        // 确保头颅列表不为空
        if (skinHashes.isEmpty()) {
            this.logger().error(LOG_PREFIX + "没有头颅需要注册");
            return;
        }
        
        // 确保Geyser缓存目录中已经有了我们的头颅文件
        if (geyserCacheDir != null) {
            logMessage("在注册头颅前确保Geyser缓存目录中已有头颅文件...");
            ensureGeyserUsesCachedFiles();
        }
        
        // 注册所有加载的头颅
        logTitle("开始注册 " + skinHashes.size() + " 个头颅");
        int count = 0;
        for (String hash : skinHashes) {
            if (hash != null && !hash.trim().isEmpty()) {
                try {
                    // 注册皮肤哈希
                    event.register(hash, SkullTextureType.SKIN_HASH);
                    count++;
                    
                    if (count % 500 == 0) {
                        logStats("已注册头颅", count, skinHashes.size());
                    }
                } catch (Exception e) {
                    this.logger().error(LOG_PREFIX + "注册头颅时出错: " + hash, e);
                }
            }
        }
        logTitle("成功注册 " + count + " 个头颅");
        
        // 强制Geyser使用我们下载的缓存文件
        if (geyserCacheDir != null) {
            logMessage("再次确保Geyser使用缓存的头颅文件...");
            ensureGeyserUsesCachedFiles();
        } else {
            this.logger().error(LOG_PREFIX + "无法确保Geyser使用缓存文件，因为缓存目录未初始化");
        }
    }
    
    /**
     * 确保Geyser使用我们缓存的头颅文件
     */
    private void ensureGeyserUsesCachedFiles() {
        try {
            // 获取Geyser的缓存目录中的player_skulls文件夹
            File geyserSkullsDir = new File(geyserCacheDir, "player_skulls");
            if (!geyserSkullsDir.exists()) {
                geyserSkullsDir.mkdirs();
                logMessage("创建Geyser头颅缓存目录: " + geyserSkullsDir.getAbsolutePath());
            }
            
            // 如果没有下载器，尝试直接从我们的缓存目录复制文件
            if (downloader == null) {
                File ourCacheDir = new File(this.dataFolder().toFile(), "cache/player_skulls");
                if (!ourCacheDir.exists()) {
                    ourCacheDir.mkdirs();
                    logMessage("创建我们的头颅缓存目录: " + ourCacheDir.getAbsolutePath());
                }
                
                if (ourCacheDir.exists() && ourCacheDir.isDirectory()) {
                    logMessage("使用缓存目录: " + ourCacheDir.getAbsolutePath());
                    
                    // 复制所有缓存的头颅文件
                    File[] cachedFiles = ourCacheDir.listFiles((dir, name) -> name.endsWith(".png"));
                    if (cachedFiles != null && cachedFiles.length > 0) {
                        int copiedCount = 0;
                        for (File sourceFile : cachedFiles) {
                            String hash = sourceFile.getName().replace(".png", "");
                            File targetFile = new File(geyserSkullsDir, sourceFile.getName());
                            
                            if (!targetFile.exists() || sourceFile.lastModified() > targetFile.lastModified()) {
                                try {
                                    Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                    copiedCount++;
                                } catch (IOException e) {
                                    this.logger().error(LOG_PREFIX + "复制头颅文件时出错: " + hash, e);
                                }
                            }
                        }
                        
                        if (copiedCount > 0) {
                            logStats("已将头颅文件复制到Geyser缓存目录", copiedCount, cachedFiles.length);
                        } else {
                            logMessage("没有需要复制的头颅文件");
                        }
                    } else {
                        logMessage("缓存目录中没有头颅文件");
                    }
                } else {
                    logMessage("缓存目录不存在或不是目录: " + ourCacheDir.getAbsolutePath());
                }
                return;
            }
            
            // 获取我们的缓存目录
            File ourSkullsDir = downloader.getCacheDir();
            logMessage("使用下载器缓存目录: " + ourSkullsDir.getAbsolutePath());
            
            // 确保所有已下载的头颅文件都在Geyser的缓存目录中
            Map<String, Boolean> downloadResults = downloader.getDownloadResults();
            int copiedCount = 0;
            int totalCount = 0;
            
            for (String hash : downloadResults.keySet()) {
                if (downloadResults.get(hash)) {  // 如果下载成功
                    totalCount++;
                    File sourceFile = new File(ourSkullsDir, hash + ".png");
                    File targetFile = new File(geyserSkullsDir, hash + ".png");
                    
                    if (sourceFile.exists() && (!targetFile.exists() || sourceFile.lastModified() > targetFile.lastModified())) {
                        try {
                            Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            copiedCount++;
                        } catch (IOException e) {
                            this.logger().error(LOG_PREFIX + "复制头颅文件时出错: " + hash, e);
                        }
                    }
                }
            }
            
            if (copiedCount > 0) {
                logStats("已将头颅文件复制到Geyser缓存目录", copiedCount, totalCount);
            } else {
                logMessage("没有需要复制的头颅文件");
            }
        } catch (Exception e) {
            this.logger().error(LOG_PREFIX + "确保Geyser使用缓存文件时出错", e);
        }
    }

    @Subscribe
    public void onPostInitialize(GeyserPostInitializeEvent event) {
        logTitle("GeyserHeadPack 插件已加载");

        logMessage(">>>>>>>>>><<<<<<<<<<");
        logMessage("A Geyser Extension By mclishang");
        logMessage("Release 1.0.0");
        logMessage("本插件用于在Geyser中高效加载自定义头颅");
        logMessage("项目地址: https://github.com/mclishang/GeyserHeadPack");
        logMessage(">>>>>>>>>><<<<<<<<<<");

        // 如果已经初始化，不需要重复初始化
        if (initialized) {
            logTitle("插件已经初始化，跳过");
            return;
        }

        // 初始化配置文件路径
        ensureConfigFilesInitialized();

        // 确保所有配置文件都存在
        initializeConfigFiles();
        
        // 加载配置文件
        loadConfiguration();
        
        // 加载头颅配置
        loadHeadsConfig();
        
        // 初始化下载器并开始下载
        if (config != null && !skinHashes.isEmpty()) {
            try {
                // 获取Geyser缓存目录
                geyserCacheDir = GeyserCacheUtil.getGeyserCacheDir(this);
                
                // 初始化下载器
                downloader = new SkullDownloader(this, config, geyserCacheDir);
                
                // 开始下载
                logTitle("开始预下载头颅皮肤文件");
                int successCount = downloader.downloadSkins(skinHashes);
                logStats("预下载完成", successCount, skinHashes.size());
                
                // 标记初始化完成
                initialized = true;
                
                // 主动将下载的文件复制到Geyser缓存目录
                logMessage("正在将下载的头颅文件复制到Geyser缓存目录...");
                ensureGeyserUsesCachedFiles();
                
                // 延迟尝试手动注册头颅
                logMessage("计划在5秒后尝试手动注册头颅...");
                scheduler.schedule(() -> {
                    logTitle("开始尝试手动注册头颅");
                    try {
                        registerHeadsManually();
                    } catch (Exception e) {
                        this.logger().error(LOG_PREFIX + "延迟注册头颅时出错", e);
                    }
                }, 5, TimeUnit.SECONDS);
            } catch (Exception e) {
                this.logger().error(LOG_PREFIX + "初始化下载器时出错", e);
            }
        } else {
            this.logger().error(LOG_PREFIX + "配置加载失败或没有头颅需要加载");
        }
    }

    @Subscribe
    public void onGeyserPreInitialize(GeyserPreInitializeEvent event) {
        logTitle("GeyserHeadPack 插件正在初始化");
        
        
        // 确保配置文件路径已初始化
        ensureConfigFilesInitialized();
        
        // 确保所有配置文件都存在
        initializeConfigFiles();
        
        // 加载配置
        loadConfiguration();
        
        // 加载头颅配置
        loadHeadsConfig();
        
        // 尝试提前获取Geyser缓存目录
        try {
            geyserCacheDir = GeyserCacheUtil.getGeyserCacheDir(this);
            logMessage("已获取Geyser缓存目录: " + geyserCacheDir.getAbsolutePath());
            
            // 提前创建Geyser头颅缓存目录
            File geyserSkullsDir = new File(geyserCacheDir, "player_skulls");
            if (!geyserSkullsDir.exists()) {
                geyserSkullsDir.mkdirs();
                logMessage("提前创建Geyser头颅缓存目录: " + geyserSkullsDir.getAbsolutePath());
            }
            
            // 在预初始化阶段就开始下载头颅
            if (config != null && !skinHashes.isEmpty()) {
                // 初始化下载器
                downloader = new SkullDownloader(this, config, geyserCacheDir);
                
                // 开始下载
                logTitle("在预初始化阶段开始下载头颅皮肤文件");
                int successCount = downloader.downloadSkins(skinHashes);
                logStats("预下载完成", successCount, skinHashes.size());
                
                // 标记初始化完成
                initialized = true;
                
                // 立即将下载的文件复制到Geyser缓存目录
                logMessage("正在将下载的头颅文件复制到Geyser缓存目录...");
                ensureGeyserUsesCachedFiles();
            }
        } catch (Exception e) {
            this.logger().error(LOG_PREFIX + "预初始化时出错", e);
        }
    }
    
    @Subscribe
    public void onGeyserShutdown(GeyserShutdownEvent event) {
        logTitle("GeyserHeadPack 插件正在关闭");
        
        // 关闭线程池
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            logMessage("线程池已关闭");
        } catch (Exception e) {
            this.logger().error(LOG_PREFIX + "关闭线程池时出错", e);
        }
        
        // 显示插件统计信息
        if (initialized) {
            logTitle("插件运行统计");
            logMessage("加载的头颅总数: " + skinHashes.size());
            if (downloader != null) {
                Map<String, Boolean> results = downloader.getDownloadResults();
                int successCount = 0;
                for (Boolean success : results.values()) {
                    if (success) {
                        successCount++;
                    }
                }
                logStats("头颅下载结果", successCount, results.size());
            }
        }
        
        logTitle("GeyserHeadPack 插件已关闭");
    }

    /**
     * 初始化所有配置文件
     */
    private void initializeConfigFiles() {
        // 初始化headpack.yml
        if (!configFile.exists()) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("headpack.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    logMessage("已创建默认headpack.yml配置文件");
                } else {
                    this.logger().error(LOG_PREFIX + "无法找到默认的headpack.yml资源文件");
                }
            } catch (IOException e) {
                this.logger().error(LOG_PREFIX + "复制headpack.yml文件时发生错误", e);
            }
        }
        
        // 初始化heads.yml
        if (!headsConfigFile.exists()) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("heads.yml")) {
                if (in != null) {
                    Files.copy(in, headsConfigFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    logMessage("已创建默认heads.yml配置文件");
                } else {
                    this.logger().error(LOG_PREFIX + "无法找到默认的heads.yml资源文件");
                }
            } catch (IOException e) {
                this.logger().error(LOG_PREFIX + "复制heads.yml文件时发生错误", e);
            }
        }
    }

    /**
     * 加载配置文件
     */
    private void loadConfiguration() {
        // 加载配置
        config = GeyserHeadPackConfig.load(configFile);
        if (config != null) {
            logMessage("已加载配置文件");
            if (config.isDebug()) {
                logMessage("调试模式已启用");
            }
            if (config.isProxyEnabled()) {
                logMessage("代理已启用: " + config.getProxyType() + " " + 
                                  config.getProxyHost() + ":" + config.getProxyPort());
            }
        }
    }

    /**
     * 加载头颅配置
     */
    private void loadHeadsConfig() {
        skinHashes.clear();
        
        try {
            // 加载头颅配置
            if (headsConfigFile.exists()) {
                readFromFile(headsConfigFile);
                logMessage("从配置文件中加载了 " + skinHashes.size() + " 个头颅");
            } else {
                this.logger().error(LOG_PREFIX + "头颅配置文件不存在: " + headsConfigFile.getAbsolutePath());
            }
        } catch (Exception e) {
            this.logger().error(LOG_PREFIX + "读取头颅配置文件时出错", e);
        }
    }
    
    /**
     * 从文件中读取头颅配置
     */
    private void readFromFile(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("文件不存在: " + file.getAbsolutePath());
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean inSkinHashesSection = false;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                if (line.equals("skin-hashes:")) {
                    inSkinHashesSection = true;
                    continue;
                }
                
                if (inSkinHashesSection && line.startsWith("-")) {
                    String hash = line.substring(1).trim();
                    if (!hash.isEmpty()) {
                        skinHashes.add(hash);
                    }
                }
            }
        }
    }
    
    /**
     * 从输入流中读取头颅配置
     */
    private void readFromInputStream(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            boolean inSkinHashesSection = false;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                if (line.equals("skin-hashes:")) {
                    inSkinHashesSection = true;
                    continue;
                }
                
                if (inSkinHashesSection && line.startsWith("-")) {
                    String hash = line.substring(1).trim();
                    if (!hash.isEmpty()) {
                        skinHashes.add(hash);
                    }
                }
            }
        }
    }
}
