package org.geyser.extension.mclishang.config;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 插件配置类
 */
public class GeyserHeadPackConfig {
    // 代理设置
    private boolean proxyEnabled = false;
    private String proxyType = "HTTP";
    private String proxyHost = "127.0.0.1";
    private int proxyPort = 7890;
    private String proxyUsername = "";
    private String proxyPassword = "";

    // 下载设置
    private int downloadThreads = 5;
    private int connectTimeout = 5000;
    private int readTimeout = 10000;
    private int retryCount = 3;
    private int retryDelay = 1000;

    // 缓存设置
    private boolean cleanUnused = true;

    // 调试模式
    private boolean debug = false;

    /**
     * 从文件加载配置
     * @param configFile 配置文件
     * @return 配置对象
     */
    public static GeyserHeadPackConfig load(File configFile) {
        GeyserHeadPackConfig config = new GeyserHeadPackConfig();
        
        if (configFile == null || !configFile.exists()) {
            return config; // 使用默认配置
        }
        
        try (FileInputStream fis = new FileInputStream(configFile);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            
            Yaml yaml = new Yaml();
            Map<String, Object> yamlData = yaml.load(isr);
            
            if (yamlData != null) {
                config.loadFromMap(yamlData);
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return config;
    }

    /**
     * 从classpath资源加载配置
     * @param resourcePath 资源路径
     * @return 配置对象
     */
    public static GeyserHeadPackConfig loadFromResource(String resourcePath) {
        GeyserHeadPackConfig config = new GeyserHeadPackConfig();
        
        try (InputStream is = GeyserHeadPackConfig.class.getClassLoader().getResourceAsStream(resourcePath);
             InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            
            Yaml yaml = new Yaml();
            Map<String, Object> yamlData = yaml.load(isr);
            
            if (yamlData != null) {
                config.loadFromMap(yamlData);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return config;
    }

    /**
     * 从Map加载配置
     * @param map YAML配置Map
     */
    @SuppressWarnings("unchecked")
    private void loadFromMap(Map<String, Object> map) {
        // 加载代理设置
        if (map.containsKey("proxy")) {
            Map<String, Object> proxy = (Map<String, Object>) map.get("proxy");
            
            if (proxy.containsKey("enabled")) {
                this.proxyEnabled = Boolean.parseBoolean(proxy.get("enabled").toString());
            }
            
            if (proxy.containsKey("type")) {
                this.proxyType = proxy.get("type").toString();
            }
            
            if (proxy.containsKey("host")) {
                this.proxyHost = proxy.get("host").toString();
            }
            
            if (proxy.containsKey("port")) {
                this.proxyPort = Integer.parseInt(proxy.get("port").toString());
            }
            
            if (proxy.containsKey("username")) {
                this.proxyUsername = proxy.get("username").toString();
            }
            
            if (proxy.containsKey("password")) {
                this.proxyPassword = proxy.get("password").toString();
            }
        }
        
        // 加载下载设置
        if (map.containsKey("download")) {
            Map<String, Object> download = (Map<String, Object>) map.get("download");
            
            if (download.containsKey("threads")) {
                this.downloadThreads = Integer.parseInt(download.get("threads").toString());
            }
            
            if (download.containsKey("connect-timeout")) {
                this.connectTimeout = Integer.parseInt(download.get("connect-timeout").toString());
            }
            
            if (download.containsKey("read-timeout")) {
                this.readTimeout = Integer.parseInt(download.get("read-timeout").toString());
            }
            
            if (download.containsKey("retry-count")) {
                this.retryCount = Integer.parseInt(download.get("retry-count").toString());
            }
            
            if (download.containsKey("retry-delay")) {
                this.retryDelay = Integer.parseInt(download.get("retry-delay").toString());
            }
        }
        
        // 加载缓存设置
        if (map.containsKey("cache")) {
            Map<String, Object> cache = (Map<String, Object>) map.get("cache");
            
            if (cache.containsKey("clean-unused")) {
                this.cleanUnused = Boolean.parseBoolean(cache.get("clean-unused").toString());
            }
        }
        
        // 加载调试模式
        if (map.containsKey("debug")) {
            this.debug = Boolean.parseBoolean(map.get("debug").toString());
        }
    }

    // Getters
    public boolean isProxyEnabled() {
        return proxyEnabled;
    }

    public String getProxyType() {
        return proxyType;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public String getProxyUsername() {
        return proxyUsername;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public int getDownloadThreads() {
        return downloadThreads;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public int getRetryDelay() {
        return retryDelay;
    }

    public boolean isCleanUnused() {
        return cleanUnused;
    }

    public boolean isDebug() {
        return debug;
    }
} 