package org.geyser.extension.mclishang.util;

import org.geysermc.geyser.api.extension.Extension;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;

/**
 * Geyser缓存工具类
 */
public class GeyserCacheUtil {
    
    /**
     * 获取Geyser的缓存目录
     * @param extension 扩展实例
     * @return 缓存目录，如果无法获取则返回扩展的数据目录
     */
    public static File getGeyserCacheDir(Extension extension) {
        try {
            // 尝试通过反射获取GeyserImpl实例
            Object geyserImpl = getGeyserImpl();
            
            if (geyserImpl != null) {
                // 尝试获取bootstrap对象
                Object bootstrap = invokeGeyserMethod(geyserImpl, "getBootstrap");
                
                if (bootstrap != null) {
                    // 尝试不同的方法名称来获取缓存目录
                    String[] possibleMethodNames = {
                        "getCacheDir", 
                        "getDataFolder", 
                        "getConfigFolder", 
                        "getDataPath",
                        "getCachePath"
                    };
                    
                    for (String methodName : possibleMethodNames) {
                        try {
                            Object result = invokeBootstrapMethod(bootstrap, methodName);
                            if (result != null) {
                                File cacheDir = null;
                                
                                if (result instanceof Path) {
                                    cacheDir = ((Path) result).toFile();
                                } else if (result instanceof File) {
                                    cacheDir = (File) result;
                                } else if (result instanceof String) {
                                    cacheDir = new File((String) result);
                                }
                                
                                if (cacheDir != null) {
                                    // 尝试在不同位置查找player_skulls目录
                                    File playerSkullsDir = new File(cacheDir, "player_skulls");
                                    if (playerSkullsDir.exists() || cacheDir.getName().equals("cache")) {
                                        extension.logger().info("已找到Geyser缓存目录: " + cacheDir.getAbsolutePath());
                                        return cacheDir;
                                    }
                                    
                                    File cacheDirInParent = new File(cacheDir, "cache");
                                    if (cacheDirInParent.exists()) {
                                        extension.logger().info("已找到Geyser缓存目录: " + cacheDirInParent.getAbsolutePath());
                                        return cacheDirInParent;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // 忽略单个方法的异常，尝试下一个方法
                            extension.logger().info("尝试方法 " + methodName + " 失败: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            extension.logger().error("无法获取Geyser缓存目录", e);
        }
        
        // 如果无法获取缓存目录，则使用扩展的数据目录作为备选
        File fallbackDir = new File(extension.dataFolder().toFile(), "cache");
        if (!fallbackDir.exists()) {
            fallbackDir.mkdirs();
        }
        extension.logger().info("使用备用缓存目录: " + fallbackDir.getAbsolutePath());
        return fallbackDir;
    }
    
    /**
     * 获取GeyserImpl实例
     * @return GeyserImpl对象
     * @throws ClassNotFoundException 类未找到异常
     * @throws NoSuchFieldException 字段未找到异常
     * @throws IllegalAccessException 非法访问异常
     */
    private static Object getGeyserImpl() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        Class<?> geyserClass = Class.forName("org.geysermc.geyser.GeyserImpl");
        Field instanceField = geyserClass.getDeclaredField("instance");
        instanceField.setAccessible(true);
        return instanceField.get(null);
    }
    
    /**
     * 调用Geyser方法
     * @param geyserImpl GeyserImpl实例
     * @param methodName 方法名称
     * @return 方法返回值
     * @throws NoSuchMethodException 方法未找到异常
     * @throws InvocationTargetException 调用目标异常
     * @throws IllegalAccessException 非法访问异常
     */
    private static Object invokeGeyserMethod(Object geyserImpl, String methodName) 
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = geyserImpl.getClass().getMethod(methodName);
        method.setAccessible(true);
        return method.invoke(geyserImpl);
    }
    
    /**
     * 调用Bootstrap方法
     * @param bootstrap Bootstrap实例
     * @param methodName 方法名称
     * @return 方法返回值
     * @throws NoSuchMethodException 方法未找到异常
     * @throws InvocationTargetException 调用目标异常
     * @throws IllegalAccessException 非法访问异常
     */
    private static Object invokeBootstrapMethod(Object bootstrap, String methodName) 
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = bootstrap.getClass().getMethod(methodName);
        method.setAccessible(true);
        return method.invoke(bootstrap);
    }
} 