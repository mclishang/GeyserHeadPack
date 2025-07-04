package org.geyser.extension.mclishang.util;

import org.geysermc.geyser.api.extension.Extension;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 国际化工具类，用于加载和获取不同语言的消息
 */
public class I18n {
    private final String locale;
    private final Map<String, String> messages = new HashMap<>();
    

    public I18n(String locale) {
        this.locale = locale;
    }
    

    public void load(Extension extension) {

        messages.clear();
        
        extension.logger().info("Loading language files. Selected locale: " + locale);
        
        loadLanguageFile(extension, "en_US");
        
        if (!locale.equals("en_US")) {
            loadLanguageFile(extension, locale);
        }
        
        extension.logger().info("Loaded " + messages.size() + " messages for locale: " + locale);
    }
    

    private void loadLanguageFile(Extension extension, String lang) {
        String resourcePath = "lang/" + lang + ".properties";
        
        extension.logger().info("Attempting to load language file: " + resourcePath);
        
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is != null) {
                Properties props = new Properties();

                try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    props.load(reader);
                    
                    int count = 0;

                    for (String key : props.stringPropertyNames()) {
                        messages.put(key, props.getProperty(key));
                        count++;
                    }
                    
                    extension.logger().info("Loaded language file: " + resourcePath + " with " + count + " messages");
                }
            } else {
                extension.logger().info("Language file not found: " + resourcePath);
            }
        } catch (IOException e) {
            extension.logger().error("Error loading language file: " + resourcePath, e);
        }
    }
    

    public String get(String key) {
        String message = messages.get(key);
        if (message == null) {
            return "Missing translation: " + key;
        }
        return message;
    }
    

    public String get(String key, Object... args) {
        String pattern = messages.get(key);
        if (pattern == null) {
            return "Missing translation: " + key;
        }
        
        try {
            return MessageFormat.format(pattern, args);
        } catch (Exception e) {
            return pattern;
        }
    }
    

    public String getLocale() {
        return locale;
    }
} 