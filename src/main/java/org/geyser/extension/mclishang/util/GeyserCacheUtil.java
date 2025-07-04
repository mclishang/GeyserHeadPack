package org.geyser.extension.mclishang.util;

import org.geysermc.geyser.api.extension.Extension;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Geyser缓存工具类
 */
public class GeyserCacheUtil {


    public static File getGeyserCacheDir(Extension extension) throws RuntimeException {
        I18n i18n = null;
        
        if (extension instanceof org.geyser.extension.mclishang.GeyserHeadPack) {
            i18n = ((org.geyser.extension.mclishang.GeyserHeadPack) extension).getI18n();
        }
        
        try {
            Object bootstrap = getGeyserBootstrap(extension);
            if (bootstrap == null) {
                logMessage(extension, i18n, "error.bootstrap_not_found");
                
                return findCacheDirManually(extension, i18n);
            }
            
            logMessage(extension, i18n, "debug.bootstrap_found", bootstrap.getClass().getName());
            
            File cacheDir = tryGetCacheDir(bootstrap, "getCacheDir");
            if (cacheDir != null) {
                logMessage(extension, i18n, "debug.cache_dir_found", cacheDir.getAbsolutePath());
                
                if (!cacheDir.exists()) {
                    logMessage(extension, i18n, "error.cache_dir_not_exist", cacheDir.getAbsolutePath());
                    
                    return findCacheDirManually(extension, i18n);
                }
                
                File playerSkullsDir = new File(cacheDir, "player_skulls");
                if (!playerSkullsDir.exists()) {
                    if (playerSkullsDir.mkdirs()) {
                        logMessage(extension, i18n, "debug.player_skulls_created");
                    } else {
                        throw new RuntimeException(getMessage(i18n, "error.create_player_skulls_dir", playerSkullsDir.getAbsolutePath()));
                    }
                }
                
                return cacheDir;
            } else {
                logMessage(extension, i18n, "debug.get_cache_dir_null");
            }
            
            File dataFolder = tryGetCacheDir(bootstrap, "getDataFolder");
            if (dataFolder != null) {
                logMessage(extension, i18n, "debug.data_folder_found", dataFolder.getAbsolutePath());
                
                File cacheDir2 = new File(dataFolder, "cache");
                
                if (!cacheDir2.exists()) {
                    logMessage(extension, i18n, "error.cache_subdir_not_exist", cacheDir2.getAbsolutePath());
                    
                    return findCacheDirManually(extension, i18n);
                }
                
                logMessage(extension, i18n, "debug.cache_dir_found", cacheDir2.getAbsolutePath());
                
                File playerSkullsDir = new File(cacheDir2, "player_skulls");
                if (!playerSkullsDir.exists()) {
                    if (playerSkullsDir.mkdirs()) {
                        logMessage(extension, i18n, "debug.player_skulls_created");
                    } else {
                        throw new RuntimeException(getMessage(i18n, "error.create_player_skulls_dir", playerSkullsDir.getAbsolutePath()));
                    }
                }
                
                return cacheDir2;
            } else {
                logMessage(extension, i18n, "debug.get_data_folder_null");
            }
            
            return findCacheDirManually(extension, i18n);
            
        } catch (Exception e) {
            logError(extension, i18n, "error.get_cache_dir", e.getMessage());
            
            return findCacheDirManually(extension, i18n);
        }
    }
    

    private static File findCacheDirManually(Extension extension, I18n i18n) throws RuntimeException {

        File currentDir = new File(".");
        logMessage(extension, i18n, "debug.current_dir", currentDir.getAbsolutePath());
        

        File cacheDir = new File(currentDir, "cache");
        if (cacheDir.exists() && cacheDir.isDirectory()) {
            logMessage(extension, i18n, "debug.cache_dir_found_current", cacheDir.getAbsolutePath());
            
            File playerSkullsDir = new File(cacheDir, "player_skulls");
            if (!playerSkullsDir.exists()) {
                if (playerSkullsDir.mkdirs()) {
                    logMessage(extension, i18n, "debug.player_skulls_created");
                } else {
                    throw new RuntimeException(getMessage(i18n, "error.create_player_skulls_dir", playerSkullsDir.getAbsolutePath()));
                }
            }
            
            return cacheDir;
        }
        
        File parentDir = currentDir.getAbsoluteFile().getParentFile();
        if (parentDir != null) {
            File parentCacheDir = new File(parentDir, "cache");
            if (parentCacheDir.exists() && parentCacheDir.isDirectory()) {
                logMessage(extension, i18n, "debug.cache_dir_found_parent", parentCacheDir.getAbsolutePath());
                
                File playerSkullsDir = new File(parentCacheDir, "player_skulls");
                if (!playerSkullsDir.exists()) {
                    if (playerSkullsDir.mkdirs()) {
                        logMessage(extension, i18n, "debug.player_skulls_created");
                    } else {
                        throw new RuntimeException(getMessage(i18n, "error.create_player_skulls_dir", playerSkullsDir.getAbsolutePath()));
                    }
                }
                
                return parentCacheDir;
            }
        }
        
        throw new RuntimeException(getMessage(i18n, "error.cache_dir_not_found"));
    }
    

    private static Object getGeyserBootstrap(Extension extension) {
        try {
            Class<?> geyserClass = Class.forName("org.geysermc.geyser.GeyserImpl");
            Field instanceField = geyserClass.getDeclaredField("instance");
            instanceField.setAccessible(true);
            Object geyserImpl = instanceField.get(null);
            
            if (geyserImpl != null) {
                Method getBootstrapMethod = geyserClass.getMethod("getBootstrap");
                return getBootstrapMethod.invoke(geyserImpl);
            }
        } catch (Exception e) {
            extension.logger().error("Error getting GeyserBootstrap instance: " + e.getMessage());
        }
        return null;
    }
    

    private static File tryGetCacheDir(Object bootstrap, String methodName) {
        try {
            Method method = bootstrap.getClass().getMethod(methodName);
            Object result = method.invoke(bootstrap);
            if (result instanceof File) {
                return (File) result;
            }
        } catch (Exception e) {
        }
        return null;
    }
    

    private static void logMessage(Extension extension, I18n i18n, String key) {
        if (i18n != null) {
            extension.logger().info(i18n.get(key));
        } else {
            extension.logger().info(key);
        }
    }
    

    private static void logMessage(Extension extension, I18n i18n, String key, Object... args) {
        if (i18n != null) {
            extension.logger().info(i18n.get(key, args));
        } else {
            extension.logger().info(key + ": " + formatArgs(args));
        }
    }
    

    private static void logError(Extension extension, I18n i18n, String key, Object... args) {
        if (i18n != null) {
            extension.logger().error(i18n.get(key, args));
        } else {
            extension.logger().error(key + ": " + formatArgs(args));
        }
    }
    
    private static String getMessage(I18n i18n, String key, Object... args) {
        if (i18n != null) {
            return i18n.get(key, args);
        } else {
            return key + ": " + formatArgs(args);
        }
    }
    
    private static String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(args[i] == null ? "null" : args[i].toString());
        }
        return sb.toString();
    }
} 