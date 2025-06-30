# GeyserHeadPack

基于Geyser的扩展插件，用于高效注册自定义头颅到Geyser中

## 功能

- 从配置文件加载头颅列表
- 自动注册头颅到Geyser
- **通过代理下载头颅皮肤文件（解决SSL错误问题）**
- **多线程下载和本地缓存，提高加载速度**
- **在Geyser启动前预先下载头颅，避免Geyser自身下载导致的网络错误**

## 使用方法

[GeyserHeadPack Usage](https://github.com/mclishang/GeyserHeadPack/blob/master/USAGE.md)

## 配置文件格式

### heads.yml
```yaml
skin-hashes:
  - 头颅1
  - 头颅2
  - 头颅3
  # 可以添加更多哈希值
```

### headpack.yml
```yaml
# GeyserHeadPack 配置文件

# 代理设置
proxy:
  # 是否启用代理
  enabled: false
  # 代理类型: HTTP, SOCKS
  type: HTTP
  # 代理地址
  host: 127.0.0.1
  # 代理端口
  port: 7897
  # 代理用户名（如果需要认证）
  username: ""
  # 代理密码（如果需要认证）
  password: ""

# 下载设置
download:
  # 线程数
  threads: 8
  # 连接超时（毫秒）
  connect-timeout: 5000
  # 读取超时（毫秒）
  read-timeout: 10000
  # 重试次数
  retry-count: 3
  # 重试间隔（毫秒）
  retry-delay: 1000

# 缓存设置
cache:
  # 是否清理未使用的缓存
  clean-unused: true

# 调试模式
debug: false
```

## 构建方法

```bash
./gradlew build
```

构建后的插件文件位于 `build/libs/` 目录下。

## 注意事项

- 需要确保Geyser配置中 `add-non-bedrock-items` 设置为 `true`
- 头颅需要是有效的Minecraft皮肤哈希值
- 如果在公开网络环境遇到下载问题，请配置代理
