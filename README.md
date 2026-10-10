# FCMFix Local (Hans Gmail)

基于 [kooritea/fcmfix](https://github.com/kooritea/fcmfix) 的 `b366825`，归档本地 ColorOS 16 / Gmail 实验版本。源码分支为 `local/coloros16-hans-gmail`。

- [下载当前 APK 与 SHA-256 校验文件](https://github.com/Avi7ii/fcmfix/releases/tag/v53-local-hans-gmail-hma-v2)
- [当前版本、验证结果及安装说明](releases/v53-local-hans-gmail-hma-v2/BUILD-INFO.md)
- [保留的 v1 实验版归档](https://github.com/Avi7ii/fcmfix/releases/tag/v53-local-hans-gmail-v1)

包名为 `com.kooritea.fcmfix.local`，可与官方版共存；两者的设置互不共享。保留上游原有处理逻辑，新增对白名单中 Gmail 的 `gmail-ls` 同步任务的定向 Hans 放行，以及 Android 16 上与 HMA-OSS 共存的广播入口兼容处理；不增加后台保活服务。

2026-10-10 的回归排查先发现远程白名单缺失；恢复白名单后，又确认 HMA-OSS 替换了 `broadcastIntentLocked` 的运行入口，绕过 FCMFix。当前 `5302` 在 Android 16 / API 36 上改用签名匹配的内层 `broadcastIntentLockedTraced`，不修改 HMA 配置，不关闭其 USB/ADB 隐藏功能。已验证重启后未手动打开 Gmail 的真实来信推送；X 尚未做端到端复测，其他系统或模块版本也需另行验证。

以下保留上游说明。

---

# fcmfix(Android 10-15 )

[![Android CI](https://github.com/kooritea/fcmfix/workflows/Android%20CI/badge.svg)](https://github.com/kooritea/fcmfix/actions)

让fcm/gcm唤醒未启动的应用进行发送通知  

### 附加功能

- 阻止Android系统在应用停止时自动移除通知栏的通知
- 在miui/hyperos(?)/OxygenOS15(?)/ColorOS15(?)上动态解除来自fcm的自启动限制
- 移除miui/hyperos对后台应用的通知限制
- 没有预期唤醒目标应用时发送提示通知

### lsposed作用域
- 在miui/hyperos上如果推送没有问题，就不需要勾选电量和性能

### 关于fcm

fcm是在Android中由google维护的一条介于google服务器与gms应用之间用于推送通知的长链接。  
一般的工作流程为应用服务器将消息发送到google服务器，google服务器将消息推送给gms应用，gms应用通过广播传递给应用，应用通过接收到的fcm消息决定是否发送通知和通知内容。  
其中gms通过fcm广播通知应用时，如果应用处于非运行状态，就会出现`Failed to broadcast to stopped app`，fcmfix主要就是解决这个问题。

### 已知问题

- 非miui/hyperos/OxygenOS15/ColorOS15系统可能需要给予目标应用类似允许自启动的权限，以及电池选项设置为不优化。
