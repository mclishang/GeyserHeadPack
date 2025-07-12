# GeyserHeadPack（Chinese）

GeyserHeadPack是一个Geyser扩展插件，用于加载和注册自定义头颅。

## 功能特性

- 自动下载和注册自定义头颅皮肤
- 精确匹配Geyser的头颅处理方式，确保100%兼容
- 支持代理设置，便于在网络受限环境中使用
- 支持中文和英文界面，根据系统语言自动选择
- 高效的多线程下载，提高加载速度
- 缓存管理，避免重复下载

## 安装方法

1. 下载最新版本的GeyserHeadPack.jar
2. 将jar文件放入Geyser的extensions目录中
3. 重启Geyser服务器

## 配置文件

### headpack.yml

```yaml
# 下载设置
download:
  # 下载线程数
  threads: 16
  # 连接超时时间(毫秒)
  connect-timeout: 5000
  # 读取超时时间(毫秒)
  read-timeout: 5000
  # 是否强制重新下载已存在的皮肤
  force-download: false
  # 是否清理未使用的缓存文件
  clean-unused: true

# 代理设置
proxy:
  # 是否启用代理
  enabled: false
  # 代理类型 (HTTP 或 SOCKS)
  type: HTTP
  # 代理主机
  host: 127.0.0.1
  # 代理端口
  port: 7897

# 调试模式
debug: false

# 语言设置 (zh_CN 或 en_US)
language: zh_CN
```

### heads.yml

```yaml
# 皮肤哈希列表
skin-hashes:
  - 2c4a72af11a0a81e5c3f5168f6cebc7a1c8a5f0e9c9c6d5a4b3c2d1e0f9a8b7c
  - 3b5a72af11a0a81e5c3f5168f6cebc7a1c8a5f0e9c9c6d5a4b3c2d1e0f9a8b7c
```

## 语言支持

插件支持中文和英文两种语言，会根据系统语言环境自动选择，也可在配置中指定。目前支持:

- 中文 (zh_CN)
- 英文 (en_US)

## 构建方法

```bash
./gradlew build
```

构建后的插件文件位于 `build/libs/` 目录下。

## 注意事项

- 需要确保Geyser配置中 `add-non-bedrock-items` 设置为 `true`
- 头颅需要是有效的Minecraft皮肤哈希值（十六进制格式）
- 如果在公开网络环境遇到下载问题，请配置代理
- 该插件完美匹配Geyser的头颅处理机制，确保头颅正确显示

## 技术细节

本插件使用与Geyser完全相同的方式处理头颅皮肤:
1. 将皮肤转换为特定的48x16格式
2. 保存到Geyser缓存目录的player_skulls文件夹中
3. 通过正确的方式注册到Geyser的头颅注册表中

这种方法确保了与Geyser的完全兼容性，避免了头颅显示错乱的问题。

---

# GeyserHeadPack (English)

GeyserHeadPack is a Geyser extension plugin for loading and registering custom skulls.

## Features

- Automatically download and register custom skull skins
- Precisely matches Geyser's skull processing method, ensuring 100% compatibility
- Support proxy settings for use in restricted network environments
- Support Chinese and English interfaces, automatically selected based on system language
- Efficient multi-threaded downloading for faster loading
- Cache management to avoid repeated downloads

## Installation

1. Download the latest version of GeyserHeadPack.jar
2. Place the jar file in Geyser's extensions directory
3. Restart the Geyser server

## Configuration Files

### headpack.yml

```yaml
# Download settings
download:
  # Number of download threads
  threads: 16
  # Connection timeout (milliseconds)
  connect-timeout: 5000
  # Read timeout (milliseconds)
  read-timeout: 5000
  # Whether to force redownload existing skins
  force-download: false
  # Whether to clean unused cache files
  clean-unused: true

# Proxy settings
proxy:
  # Whether to enable proxy
  enabled: false
  # Proxy type (HTTP or SOCKS)
  type: HTTP
  # Proxy host
  host: 127.0.0.1
  # Proxy port
  port: 7897

# Debug mode
debug: false

# Language setting (zh_CN or en_US)
language: en_US
```

### heads.yml

```yaml
# Skin hash list
skin-hashes:
  - 2c4a72af11a0a81e5c3f5168f6cebc7a1c8a5f0e9c9c6d5a4b3c2d1e0f9a8b7c
  - 3b5a72af11a0a81e5c3f5168f6cebc7a1c8a5f0e9c9c6d5a4b3c2d1e0f9a8b7c
```

## Language Support

The plugin supports both Chinese and English languages, automatically selected based on the system language environment or specified in the configuration. Currently supported:

- Chinese (zh_CN)
- English (en_US)

## Build Instructions

```bash
./gradlew build
```

The built plugin file will be located in the `build/libs/` directory.

## Notes

- Ensure that `add-non-bedrock-items` is set to `true` in your Geyser configuration
- Skulls must be valid Minecraft skin hash values (hexadecimal format)
- If you encounter download issues in public network environments, please configure a proxy
- This plugin perfectly matches Geyser's skull processing mechanism, ensuring correct skull display

## Technical Details

This plugin processes skull skins in exactly the same way as Geyser:
1. Converts skins to a specific 48x16 format
2. Saves them to the player_skulls folder in Geyser's cache directory
3. Registers them to Geyser's skull registry in the correct manner

This approach ensures complete compatibility with Geyser, avoiding issues with distorted skull displays.
