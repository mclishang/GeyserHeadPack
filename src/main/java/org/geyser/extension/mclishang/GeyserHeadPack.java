package org.geyser.extension.mclishang;

import org.geyser.extension.mclishang.config.GeyserHeadPackConfig;
import org.geyser.extension.mclishang.downloader.SkullDownloader;
import org.geyser.extension.mclishang.util.GeyserCacheUtil;
import org.geyser.extension.mclishang.util.I18n;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomSkullsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomSkullsEvent.SkullTextureType;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserShutdownEvent;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.extension.Extension;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.yaml.snakeyaml.Yaml;
import java.util.Map;

/**
 * GeyserHeadPack扩展 - 用于加载和注册自定义头颅
 * 
 * @author mclishang
 * @version 1.0.0
 * @since 2025-07-01
 */
public class GeyserHeadPack implements Extension {

    private static final String HEADS_CONFIG_FILENAME = "heads.yml";
    private static final String CONFIG_FILENAME = "headpack.yml";
    private static final int LOG_INTERVAL = 500;
    private static final String GITHUB_URL = "https://github.com/mclishang/GeyserHeadPack";
    
    private File headsConfigFile;
    private File configFile;
    private final List<String> skinHashes = new ArrayList<>();
    private GeyserHeadPackConfig config;
    private SkullDownloader downloader;
    private boolean initialized = false;
    private File geyserCacheDir;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private I18n i18n;
    private boolean disabled = false;
    private String version;
    
    private void logMessage(String key) {
        this.logger().info(i18n.get(key));
    }
    
    private void logMessage(String key, Object... args) {
        this.logger().info(i18n.get(key, args));
    }
    
    private void logTitle(String key) {
        this.logger().info(i18n.get(key));
    }
    
    private void logStats(String key, int count, int total) {
        this.logger().info(i18n.get(key, count, total));
    }
    
    private void logError(String key) {
        this.logger().error(i18n.get(key));
    }
    
    private void logError(String key, Object... args) {
        this.logger().error(i18n.get(key, args));
    }

    private void ensureConfigFilesInitialized() {
        if (headsConfigFile == null || configFile == null) {
            Path dataFolder = this.dataFolder();
            if (!dataFolder.toFile().exists()) {
                dataFolder.toFile().mkdirs();
            }
            
            headsConfigFile = dataFolder.resolve(HEADS_CONFIG_FILENAME).toFile();
            configFile = dataFolder.resolve(CONFIG_FILENAME).toFile();
        }
    }

    @Subscribe
    public void onGeyserDefineCustomSkulls(GeyserDefineCustomSkullsEvent event) {
        if (disabled) {
            logError("error.registration_skipped");
            return;
        }
        
        logTitle("title.register_skulls");
        
        // 对每个皮肤哈希进行检查和注册
        int registeredCount = 0;
        int skippedCount = 0;
        
        for (String hash : skinHashes) {
            try {
                String cleanHash = hash.trim();
                if (cleanHash.isEmpty()) {
                    continue;
                }
                
                // 验证哈希格式
                if (!cleanHash.matches("^[a-fA-F0-9]+$")) {
                    logError("error.invalid_hash_format", cleanHash);
                    skippedCount++;
                    continue;
                }
                
                // 确保皮肤文件存在
                File playerSkullsDir = new File(geyserCacheDir, "player_skulls");
                File skinFile = new File(playerSkullsDir, cleanHash + ".png");
                if (!skinFile.exists() || skinFile.length() == 0) {
                    logError("error.skin_file_missing", cleanHash, skinFile.getAbsolutePath());
                    skippedCount++;
                    continue;
                }
                
                // 使用Geyser的皮肤哈希注册方法
                event.register(cleanHash, GeyserDefineCustomSkullsEvent.SkullTextureType.SKIN_HASH);
                registeredCount++;
                
                if (registeredCount % LOG_INTERVAL == 0) {
                    logStats("stats.registered", registeredCount, skinHashes.size());
                }
            } catch (Exception e) {
                logError("error.register_skull", hash, e.getMessage());
                skippedCount++;
            }
        }
        
        logMessage("message.registration_complete", registeredCount);
        if (skippedCount > 0) {
            logMessage("message.registration_skipped", skippedCount);
        }
    }
    
    @Subscribe
    public void onGeyserPreInitialize(GeyserPreInitializeEvent event) {
        loadVersion();
        ensureConfigFilesInitialized();
        initializeConfigFiles();
        loadConfiguration();
        
        initializeI18n();
        
        displayCopyright();
        
        logTitle("title.initializing");
        
        try {
            try {
                // 获取Geyser缓存目录
                geyserCacheDir = GeyserCacheUtil.getGeyserCacheDir(this);
                if (geyserCacheDir == null || !geyserCacheDir.exists()) {
                    logError("error.cache_dir_not_found", "Cache directory is null or does not exist");
                    logError("error.plugin_disabled");
                    disabled = true;
                    return;
                }
                
                // 确保player_skulls目录存在，这是Geyser要求的结构
                File playerSkullsDir = new File(geyserCacheDir, "player_skulls");
                if (!playerSkullsDir.exists()) {
                    playerSkullsDir.mkdirs();
                }
                
                // 加载头颅配置
                loadHeadsConfig();
                
                if (config != null && !skinHashes.isEmpty()) {
                    // 创建下载器实例并下载皮肤
                    downloader = new SkullDownloader(this, config, geyserCacheDir, i18n);
                    logTitle("title.predownload");
                    int successCount = downloader.downloadSkins(skinHashes);
                    logStats("stats.download_complete", successCount, skinHashes.size());
                    
                    if (successCount > 0) {
                        logMessage("message.ready_for_registration", successCount);
                    } else {
                        logError("error.no_skins_downloaded");
                    }
                    
                    initialized = true;
                } else {
                    logMessage("message.no_hashes");
                    initialized = true;
                }
            } catch (Exception e) {
                logError("error.initializing_downloader", e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            logError("error.initialization_failed", e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadVersion() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("extension.yml")) {
            if (is != null) {
                Yaml yaml = new Yaml();
                Map<String, Object> config = yaml.load(is);
                version = (String) config.get("version");
                if (version == null) {
                    version = "unknown";
                    this.logger().info("Failed to read version from extension.yml, using default");
                }
            } else {
                version = "unknown";
                this.logger().info("Failed to find extension.yml, using default version");
            }
        } catch (Exception e) {
            version = "unknown";
            this.logger().info("Error loading version from extension.yml: " + e.getMessage());
        }
    }
    
    private void displayCopyright() {
        String[] copyrightLines = {
            "§a§l============================================§r",
            "§b§lGeyserHeadPack §f[§e" + version + "§f]",
            "§f" + GITHUB_URL,
            i18n.get("copyright.description"),
            "§a§l============================================§r"
        };
        
        for (String line : copyrightLines) {
            this.logger().info(line);
        }
    }
    
    private void initializeI18n() {
        String language = "zh_CN";
        
        if (config != null && config.getLanguage() != null && !config.getLanguage().isEmpty()) {
            language = config.getLanguage();
        }
        
        i18n = new I18n(language);
        i18n.load(this);
        
        this.logger().info("Using language: " + language);
    }

    @Subscribe
    public void onPostInitialize(GeyserPostInitializeEvent event) {
        if (disabled) {
            return;
        }
        
        logMessage("message.plugin_loaded");

        if (!initialized) {
            logError("error.init_failed");
            retryInitialization();
        }
    }
    
    private void retryInitialization() {
        ensureConfigFilesInitialized();
        initializeConfigFiles();
        loadConfiguration();
        loadHeadsConfig();
        
        if (config != null && !skinHashes.isEmpty() && geyserCacheDir != null) {
            try {
                if (downloader == null) {
                    downloader = new SkullDownloader(this, config, geyserCacheDir, i18n);
                }
                
                logTitle("title.retry_download");
                int successCount = downloader.downloadSkins(skinHashes);
                logStats("stats.retry_complete", successCount, skinHashes.size());
                
                initialized = true;
            } catch (Exception e) {
                logError("error.retry_init", e.getMessage());
            }
        }
    }

    @Subscribe
    public void onGeyserShutdown(GeyserShutdownEvent event) {
        logTitle("title.shutdown");
        
        scheduler.shutdownNow();
    }

    private void initializeConfigFiles() {
        ensureConfigFilesInitialized();
        
        createConfigFileIfNotExists(headsConfigFile, HEADS_CONFIG_FILENAME, "message.create_heads_file");
        createConfigFileIfNotExists(configFile, CONFIG_FILENAME, "message.create_config_file");
    }
    
    private void createConfigFileIfNotExists(File file, String resourceName, String logKey) {
        if (!file.exists()) {
            if (i18n != null) {
                logMessage(logKey);
            } else {
                this.logger().info("Creating " + resourceName);
            }
            
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
                if (is != null) {
                    file.getParentFile().mkdirs();
                    Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    if (i18n != null) {
                        logError("error.resource_not_found", resourceName);
                    } else {
                        this.logger().error("Resource not found: " + resourceName);
                    }
                }
            } catch (IOException e) {
                if (i18n != null) {
                    logError("error.create_file", resourceName, e.getMessage());
                } else {
                    this.logger().error("Error creating " + resourceName + ": " + e.getMessage());
                }
            }
        }
    }

    private void loadConfiguration() {
        config = GeyserHeadPackConfig.load(configFile);
        if (config != null) {
            if (i18n != null) {
                logMessage("message.config_loaded");
                if (config.isDebug()) {
                    logMessage("message.debug_enabled");
                }
            } else {
                this.logger().info("Configuration loaded");
                if (config.isDebug()) {
                    this.logger().info("Debug mode enabled");
                }
            }
        }
    }

    private void loadHeadsConfig() {
        skinHashes.clear();
        
        try {
            if (headsConfigFile.exists()) {
                readFromFile(headsConfigFile);
                logMessage("message.heads_loaded", skinHashes.size());
            } else {
                logError("error.heads_file_not_found", headsConfigFile.getAbsolutePath());
            }
        } catch (Exception e) {
            logError("error.reading_heads_file", e.getMessage());
        }
    }
    
    private void readFromFile(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException(i18n.get("error.file_not_found", file.getAbsolutePath()));
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
    
    public I18n getI18n() {
        return i18n;
    }
}
