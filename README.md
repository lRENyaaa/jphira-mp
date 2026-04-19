# jphira-mp
Java 实现的 [phira-mp](https://github.com/TeamFlos/phira-mp) 服务端，为性能与扩展性的平衡而生

## ⚙️ 特性
* Java 实现
* 基于 [netty](https://github.com/netty/netty)
* 拥有可扩展的插件系统
* 正确实现原始逻辑

## 🚀 使用方法

运行 jphira-mp 与运行 Minecraft 服务端类似。

1. 安装 Java 17 或更高版本的 JDK（推荐 **Java 21** 以上）
2. 前往 [Release 页面](https://github.com/lRENyaaa/jphira-mp/releases) 下载最新版本
3. 在命令行中运行：

``` bash
java -jar jphira-mp-<version>.jar --port 12346
```

当前 jphira-mp 可用的命令行参数:
* `--help`: 显示帮助信息
* `--port <port>`: 指定服务器监听端口，默认为 `12346`
* `--host <host>`: 指定服务器监听地址，默认为 `0.0.0.0`
* `--plugin <folder>`: 指定插件目录，默认为 `plugins`
* `--proxy-protocol`: 启用 Proxy Protocol 支持（用于代理等，如: [此内容](https://doc.natfrp.com/bestpractice/realip.html)），默认为 `false`
* `--language`: 设置服务器默认的玩家语言，默认为 `zh-CN`

关闭 jphira-mp 同样与 Minecraft 服务端类似，在控制台输入 `stop` 命令即可关闭服务器。

## 🔌 插件开发
[![](https://jitpack.io/v/lRENyaaa/jphira-mp.svg)](https://jitpack.io/#lRENyaaa/jphira-mp)  
jphira-mp 在 [JitPack](https://jitpack.io/) 上可用

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>
```

```xml
<dependency>
    <groupId>com.github.lRENyaaa</groupId>
    <artifactId>jphira-mp</artifactId>
    <version>1.0.0-dev-20260328-01</version>
</dependency>
```

通过 [jphira-mp-example-plugin](https://github.com/lRENyaaa/jphira-mp-example-plugin) 了解API基本用法。

**请注意: jphira-mp 的尚未稳定，当前插件API可能会频繁变更**

## 📜 致谢
jphira-mp 基于如下项目:
* [jphira-mp-protocol](https://github.com/lRENyaaa/jphira-mp-protocol) - 基础协议实现
* [log4j2](https://github.com/apache/logging-log4j2) - 日志框架
* [netty](https://github.com/netty/netty) - 网络框架
* [orbit](https://github.com/MeteorDevelopment/orbit) - 事件系统

## 💬 开源协议
项目使用 LGPL v3 协议开源，见 [LICENSE](./LICENSE)  

Copyright (C) 2026 lRENyaaa
