package org.geyser.extension.mclishang.config;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * GeyserHeadPack 配置类
 */
public class GeyserHeadPackConfig {
    private final int downloadThreads;
    private final int connectTimeout;
    private final int readTimeout;
    private final boolean forceDownload;
    private final boolean cleanUnused;
    private final boolean proxyEnabled;
    private final String proxyType;
    private final String proxyHost;
    private final int proxyPort;
    private final boolean debug;
    private final String language;


    public GeyserHeadPackConfig(int downloadThreads, int connectTimeout, int readTimeout, 
                               boolean forceDownload, boolean cleanUnused, 
                               boolean proxyEnabled, String proxyType, String proxyHost, int proxyPort, 
                               boolean debug, String language) {
        this.downloadThreads = downloadThreads;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.forceDownload = forceDownload;
        this.cleanUnused = cleanUnused;
        this.proxyEnabled = proxyEnabled;
        this.proxyType = proxyType;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.debug = debug;
        this.language = language;
    }
    

    public static GeyserHeadPackConfig load(File file) {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            return loadFromInputStream(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    

    public static GeyserHeadPackConfig load(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        
        return loadFromInputStream(inputStream);
    }
    

    @SuppressWarnings("unchecked")
    private static GeyserHeadPackConfig loadFromInputStream(InputStream inputStream) {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(inputStream);
            
            boolean debug = getBooleanValue(config, "debug", false);
            
            String language = getStringValue(config, "language", "zh_CN");
            
            Map<String, Object> downloadConfig = getMapValue(config, "download");
            int downloadThreads = getIntValue(downloadConfig, "threads", 16);
            int connectTimeout = getIntValue(downloadConfig, "connect-timeout", 5000);
            int readTimeout = getIntValue(downloadConfig, "read-timeout", 5000);
            boolean forceDownload = getBooleanValue(downloadConfig, "force-download", false);
            boolean cleanUnused = getBooleanValue(downloadConfig, "clean-unused", true);
            
            Map<String, Object> proxyConfig = getMapValue(config, "proxy");
            boolean proxyEnabled = getBooleanValue(proxyConfig, "enabled", false);
            String proxyType = getStringValue(proxyConfig, "type", "HTTP");
            String proxyHost = getStringValue(proxyConfig, "host", "127.0.0.1");
            int proxyPort = getIntValue(proxyConfig, "port", 7897);
            
            return new GeyserHeadPackConfig(
                downloadThreads, connectTimeout, readTimeout, 
                forceDownload, cleanUnused, 
                proxyEnabled, proxyType, proxyHost, proxyPort, 
                debug, language
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    

    private static boolean getBooleanValue(Map<String, Object> map, String key, boolean defaultValue) {
        if (map == null) {
            return defaultValue;
        }
        
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        
        return defaultValue;
    }
    

    private static int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        if (map == null) {
            return defaultValue;
        }
        
        Object value = map.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        
        return defaultValue;
    }
    

    private static String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map == null) {
            return defaultValue;
        }
        
        Object value = map.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        
        return value != null ? value.toString() : defaultValue;
    }
    

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getMapValue(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        
        Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        
        return null;
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


    public boolean isForceDownload() {
        return forceDownload;
    }

    /**
     * 是否清理未使用的缓存
     * @return 是否清理未使用的缓存
     */
    public boolean isCleanUnused() {
        return cleanUnused;
    }

    /**
     * 是否启用代理
     * @return 是否启用代理
     */
    public boolean isProxyEnabled() {
        return proxyEnabled;
    }

    /**
     * 获取代理类型
     * @return 代理类型
     */
    public String getProxyType() {
        return proxyType;
    }

    /**
     * 获取代理主机
     * @return 代理主机
     */
    public String getProxyHost() {
        return proxyHost;
    }

    /**
     * 获取代理端口
     * @return 代理端口
     */
    public int getProxyPort() {
        return proxyPort;
    }

    /**
     * 是否启用调试模式
     * @return 是否启用调试模式
     */
    public boolean isDebug() {
        return debug;
    }
    
    /**
     * 获取语言设置
     * @return 语言设置
     */
    public String getLanguage() {
        return language;
    }
} 