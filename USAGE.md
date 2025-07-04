# 使用GeyserHeadPack（Chinese）

## 安装

1. 下载最新的GeyserHeadPack插件(.jar文件)
2. 将插件文件放入Geyser的扩展文件夹中
   - Spigot: `plugins/Geyser-Spigot/extensions/`
   - BungeeCord: `plugins/Geyser-BungeeCord/extensions/`
   - Velocity: `plugins/Geyser-Velocity/extensions/`
   - Standalone: `extensions/`
3. 启动或重启服务器
4. 插件会自动创建配置文件 `heads.yml` 和 `headpack.yml`

## 配置

1. 打开 `heads.yml` 配置文件
2. 在 `skin-hashes` 部分添加您想使用的头颅皮肤哈希值
3. 保存文件并重启服务器或使用 `/geyser reload` 命令(如果可用)

### 头颅哈希值格式

头颅哈希值必须是有效的十六进制字符串，例如：
```
3b5a72af11a0a81e5c3f5168f6cebc7a1c8a5f0e9c9c6d5a4b3c2d1e0f9a8b7c
```

无效的哈希值将被跳过，并在日志中显示警告信息。

## 网络代理

如果你的服务器无法直接访问皮肤服务器(textures.minecraft.net)或遇到SSL错误，可以配置代理：

1. 打开 `headpack.yml` 配置文件
2. 修改代理设置部分：
   ```yaml
   proxy:
     enabled: true  # 设置为true启用代理
     type: HTTP  # 或者SOCKS
     host: your-proxy-host
     port: your-proxy-port
     username: ""  # 如果需要认证，填写用户名
     password: ""  # 如果需要认证，填写密码
   ```
3. 保存文件并重启服务器

## 调整下载设置

如果需要，你可以在`headpack.yml`中调整下载设置：

```yaml
download:
  threads: 8  # 增加线程数可以加快下载速度
  connect-timeout: 5000  # 连接超时（毫秒）
  read-timeout: 10000  # 读取超时（毫秒）
  retry-count: 3  # 下载失败重试次数
  retry-delay: 1000  # 重试间隔（毫秒）
```

## 语言设置

可以在`headpack.yml`中设置插件语言:

```yaml
# 语言设置 (zh_CN 或 en_US)
language: zh_CN
```

## 获取头颅哈希值

头颅哈希可以通过以下方式获取:

1. 如果使用Paper服务器，手持头颅物品并执行命令 `/paper dumpitem`
2. 使用 [GeyserHeads](https://github.com/Hahaa13/GeyserHeads)
3. 从皮肤URL中提取，例如：
   `https://textures.minecraft.net/texture/3b5a72af11a0a81e5c3f5168f6cebc7a1c8a5f0e9c9c6d5a4b3c2d1e0f9a8b7c`
   哈希值就是URL最后的部分。

## 故障排除

### 头颅显示错乱或不显示

- 确保您使用的是最新版本的GeyserHeadPack，我们已修复了与Geyser的兼容性问题
- 检查皮肤哈希值是否正确（必须是有效的十六进制字符串）
- 启用调试模式 (`debug: true`) 查看详细日志，确认头颅是否正确注册
- 如果使用旧版本，皮肤可能以错误的格式缓存，请设置 `force-download: true` 强制重新下载
- 使用最新版本的Geyser（本插件与新版本Geyser兼容性更好）

### 网络问题

- 确保您的服务器可以访问 `textures.minecraft.net`
- 如果遇到下载错误，尝试启用代理并配置正确的代理服务器
- 如果下载仍然失败，可以尝试增加重试次数和连接超时时间
- 启用调试模式可以查看更详细的日志: `debug: true`

### 其他注意事项

- 确保您的Geyser配置中 `add-non-bedrock-items` 设置为 `true`
- 如果更改了配置，请重启服务器或使用 `/geyser reload` 命令
- 查看服务器控制台日志获取更详细的错误信息

## 工作原理

GeyserHeadPack插件通过以下方式解决Geyser头颅显示问题：

1. 在Geyser初始化前，插件会先加载所有头颅配置
2. 使用可配置的多线程下载（支持代理）预先获取头颅皮肤
3. 将这些皮肤文件以**Geyser期望的格式**处理后缓存到Geyser的缓存目录中
4. 当Geyser尝试加载这些头颅时，会发现文件已经存在，直接使用缓存文件而不会尝试再次下载
5. 由于文件格式与Geyser预期完全匹配，因此头颅能够正确显示

本插件完美模拟了Geyser的头颅处理流程，确保了100%的兼容性，解决了因格式不匹配导致的显示问题。

---

# Using GeyserHeadPack (English)

## Installation

1. Download the latest GeyserHeadPack plugin (.jar file)
2. Place the plugin file in Geyser's extensions folder
   - Spigot: `plugins/Geyser-Spigot/extensions/`
   - BungeeCord: `plugins/Geyser-BungeeCord/extensions/`
   - Velocity: `plugins/Geyser-Velocity/extensions/`
   - Standalone: `extensions/`
3. Start or restart your server
4. The plugin will automatically create configuration files `heads.yml` and `headpack.yml`

## Configuration

1. Open the `heads.yml` configuration file
2. Add the skull skin hashes you want to use in the `skin-hashes` section
3. Save the file and restart the server or use the `/geyser reload` command (if available)

### Skull Hash Format

Skull hashes must be valid hexadecimal strings, for example:
```
3b5a72af11a0a81e5c3f5168f6cebc7a1c8a5f0e9c9c6d5a4b3c2d1e0f9a8b7c
```

Invalid hashes will be skipped and a warning will be displayed in the log.

## Network Proxy

If your server cannot directly access the skin server (textures.minecraft.net) or encounters SSL errors, you can configure a proxy:

1. Open the `headpack.yml` configuration file
2. Modify the proxy settings section:
   ```yaml
   proxy:
     enabled: true  # Set to true to enable proxy
     type: HTTP  # or SOCKS
     host: your-proxy-host
     port: your-proxy-port
     username: ""  # Fill in username if authentication is required
     password: ""  # Fill in password if authentication is required
   ```
3. Save the file and restart the server

## Adjusting Download Settings

If needed, you can adjust download settings in `headpack.yml`:

```yaml
download:
  threads: 8  # Increase thread count to speed up downloads
  connect-timeout: 5000  # Connection timeout (milliseconds)
  read-timeout: 10000  # Read timeout (milliseconds)
  retry-count: 3  # Number of retry attempts for failed downloads
  retry-delay: 1000  # Delay between retries (milliseconds)
```

## Language Setting

You can set the plugin language in `headpack.yml`:

```yaml
# Language setting (zh_CN or en_US)
language: en_US
```

## Getting Skull Hash Values

Skull hashes can be obtained through the following methods:

1. If using a Paper server, hold the skull item and execute the command `/paper dumpitem`
2. Use [GeyserHeads](https://github.com/Hahaa13/GeyserHeads)
3. Extract from skin URL, for example:
   `https://textures.minecraft.net/texture/3b5a72af11a0a81e5c3f5168f6cebc7a1c8a5f0e9c9c6d5a4b3c2d1e0f9a8b7c`
   The hash is the last part of the URL.

## Troubleshooting

### Distorted or Missing Skulls

- Make sure you're using the latest version of GeyserHeadPack, which has fixed compatibility issues with Geyser
- Check if the skin hash is correct (must be a valid hexadecimal string)
- Enable debug mode (`debug: true`) to see detailed logs, confirming if the skulls are properly registered
- If using an older version, skins might be cached in the wrong format, set `force-download: true` to force re-download
- Use the latest version of Geyser (this plugin has better compatibility with newer versions)

### Network Issues

- Make sure your server can access `textures.minecraft.net`
- If you encounter download errors, try enabling the proxy and configuring the correct proxy server
- If downloads still fail, try increasing the retry count and connection timeout
- Enable debug mode to see more detailed logs: `debug: true`

### Other Notes

- Make sure `add-non-bedrock-items` is set to `true` in your Geyser configuration
- If you change the configuration, restart the server or use the `/geyser reload` command
- Check the server console logs for more detailed error information

## How It Works

The GeyserHeadPack plugin solves Geyser's skull display issues by:

1. Loading all skull configurations before Geyser initializes
2. Pre-fetching skull skins using configurable multi-threaded downloads (with proxy support)
3. Processing these skins into **the exact format Geyser expects** and caching them in Geyser's cache directory
4. When Geyser attempts to load these skulls, it finds the files already exist and uses the cached files without attempting to download again
5. Since the file format matches Geyser's expectations perfectly, the skulls display correctly

This plugin perfectly mimics Geyser's skull processing workflow, ensuring 100% compatibility and solving display issues caused by format mismatches.
