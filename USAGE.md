# 使用GeyserHeadPack

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

## 获取头颅哈希值

头颅哈希可以通过以下方式获取:

1. 如果使用Paper服务器，手持头颅物品并执行命令 `/paper dumpitem`
2. 使用 [GeyserHeads](https://github.com/Hahaa13/GeyserHeads)

## 故障排除

- 确保您的Geyser配置中 `add-non-bedrock-items` 设置为 `true`
- 如果遇到下载错误，尝试启用代理并配置正确的代理服务器
- 如果下载仍然失败，可以尝试增加重试次数和连接超时时间
- 启用调试模式可以查看更详细的日志: `debug: true`

## 工作原理

GeyserHeadPack插件通过以下方式解决Geyser下载头颅时的网络问题：

1. 在Geyser初始化前，插件会先加载所有头
2. 使用可配置的多线程下载（支持代理）预先获取头颅皮肤
3. 将这些皮肤文件缓存到Geyser的缓存目录中
4. 当Geyser尝试加载这些头颅时，会发现文件已经存在，直接使用缓存文件而不会尝试再次下载
