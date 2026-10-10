# FCMFix Local — Hans Gmail + HMA compatibility v2

## 版本与资产

- 包名：`com.kooritea.fcmfix.local`
- versionCode：`5302`
- versionName：`53-b366825-local-hans-gmail-hma-v2`
- 源码分支：`local/coloros16-hans-gmail`
- 发布标签：`v53-local-hans-gmail-hma-v2`
- APK：`fcmfix-local-53-b366825-hans-gmail-hma-v2.apk`，7,587,282 字节
- APK SHA-256：`f2dde363febf848a675905967bf20ea0bac15c6c9bab2471a67838e02bd97adf`
- 签名证书 SHA-256：`0ce547348816a2d750e7c9786bb50d74c7d943cea8ec41bd1965672f4d34f236`，与 v1 一致；私钥不上传。

Release 附带 APK、`SHA256SUMS` 和本说明；标签对应本次源码。旧版 Release 保留不变。

## 最小兼容修复

HMA-OSS oss-173 的全局广播钩子覆盖了 `BroadcastController.broadcastIntentLocked` 的 ART 入口，导致同一位置的 FCMFix 钩子不执行。普通应用白名单不会撤销这个系统级钩子。

本版仅在 API 36 上优先挂钩内层 `broadcastIntentLockedTraced`，先检查 26 个参数、Intent/appOp/BroadcastOptions 的位置及 int 返回值；不匹配时保留原入口选择逻辑。HMA 继续处理外层广播，FCMFix 继续按原有白名单和 FCM action 条件执行；未勾选应用和非 FCM 广播不获得新的放行。日志额外记录所选方法名。

没有修改 HMA、代理或系统后台设置，没有关闭 HMA 的 USB/ADB 隐藏功能。原有 GMS 重连、ColorOS 解冻、通知保留、自启动限制处理和 Gmail Hans 放行代码保留，未添加保活或定时轮询机制。

## 已完成验证

2026-10-10，OnePlus PKX110 / ColorOS `PKX110_16.0.10.500(CN01)`，Android 16，KernelSU，LSPosed 2.2.1 (7912)，HMA-OSS oss-173：

- 编译、`lintDebug`、入口选择的 10 项 JVM 回归测试、APK 签名检查通过。
- 覆盖安装后仍启用系统框架作用域；5 条远程配置记录（含 16 项白名单）逐字节不变，HMA 配置校验值不变。
- 重启后，LSPosed 记录新入口的实际地址与预期地址一致。
- 仅发给模块自身的空测试广播成功加入 `FLAG_INCLUDE_STOPPED_PACKAGES`，appOp 为 11；这项检查不等于真实推送测试。
- Gmail 在重启后未被手动打开，测试前仍为 stopped 状态。22:40:42（北京时间）的真实来信依次触发 GMS 接收、FCMFix 唤醒/解冻、ColorOS 广播放行及 Gmail `gmail-ls` Hans 同步放行；GMS 记录广播成功，广播投递耗时 295 ms，用户确认收到通知。295 ms 不是从发信到通知展示的总延迟。

X 尚未做端到端复测；上述结果不代表其他 ROM、HMA/LSPosed 版本或未来升级后的兼容性。原始设备日志、配置、邮件信息和内存转储不发布。

## 安装与复测

1. 下载 APK 与 `SHA256SUMS`，运行 `shasum -a 256 -c SHA256SUMS`。
2. 覆盖安装 v1，不要先卸载。确认 LSPosed 中模块启用且作用域包含系统框架，然后重启。
3. 保留模块中原有的 Gmail、X 等应用勾选。无需为此关闭 HMA 或改变其应用白名单。
4. 重启解锁后给现有初始化逻辑约一分钟，再在不手动打开 Gmail 的情况下发新邮件复测。

这是 LSPosed APK，不是 KSU/Magisk ZIP；自行构建时必须使用原签名才能覆盖既有安装。

## 构建与入口回归测试

本次使用 JDK 21、Android SDK 36、Gradle 8.13 和 Android Gradle Plugin 8.13.0：

```sh
./gradlew :app:assembleDebug :app:lintDebug
java tests/BroadcastHookSelectionTest.java app/src/main/java/com/kooritea/fcmfix/xposed/BroadcastFix.java
```

JVM 测试从实际源码提取入口选择方法，用最小反射签名夹具检查 API 范围、缺失/不匹配签名回退及单次挂钩；不模拟 ART、HMA 或实际通知投递。重编译产物的字节哈希不保证与本 Release 一致。
