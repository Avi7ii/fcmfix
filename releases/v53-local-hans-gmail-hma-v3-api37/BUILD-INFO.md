# FCMFix Local 5303 — Android 17 / API 37 compatibility

## 版本与资产

- 包名：`com.kooritea.fcmfix.local`
- versionCode：`5303`
- versionName：`53-b366825-local-hans-gmail-hma-v3-api37`
- 源码分支：`local/coloros16-hans-gmail`
- 发布标签：`v53-local-hans-gmail-hma-v3-api37`
- APK：`fcmfix-local-53-b366825-hans-gmail-hma-v3-api37.apk`，7,587,298 字节
- APK SHA-256：`0809e8443629390acac7b7a793e59e271f7b15dd261abda02031565a68849fe6`
- 签名证书 SHA-256：`0ce547348816a2d750e7c9786bb50d74c7d943cea8ec41bd1965672f4d34f236`，与 v1/v2 一致。

Release 包含 APK、`SHA256SUMS` 和本说明；发布 APK 与本次手机上实测通过的安装包哈希一致。旧版 Release 保留不变，签名私钥及设备原始日志、配置和邮件信息不上传。

## 最小改动

v2 只在 API 36 启用内层广播入口，升级 API 37 后会回退到被 HMA-OSS 覆盖的外层入口。本次读取新系统框架确认：内层 `BroadcastController.broadcastIntentLockedTraced` 仍具有相同的 26 参数签名。

生产逻辑仅将兼容条件从 API 36 扩展为 API 36 或 37。保留 Intent 索引 3、appOp 索引 13、BroadcastOptions 索引 14、int 返回值及参数数量校验；不匹配仍回退原路径。API 35、38 等其他版本的入口选择行为不变。

未修改 HMA、代理或系统后台设置。原有白名单、GMS 重连、ColorOS 解冻、通知保留、自启动限制处理和 Gmail Hans 放行代码不变；未添加保活或定时轮询。

## 验证结果

2026-10-18，OnePlus PKX110 / `PKX110_17.0.0.104(CN01)` / API 37，LSPosed 2.2.1 (7912)，HMA-OSS oss-173：

- 先用新增 API 37 测试复现 v2 的错误入口选择；修复后全部 17 项入口选择测试通过，编译、lint、APK 签名检查通过。
- 覆盖安装并重启后，模块仍启用系统框架作用域，原有远程配置与 16 项白名单不变，HMA 配置校验值不变。
- LSPosed 显示实际挂钩入口为 `broadcastIntentLockedTraced`，实际入口地址与预期一致；模块自身的空广播测试成功加入唤醒标志，appOp 为 11。
- 重启后未手动打开 Gmail，测试前 Gmail 仍为 stopped 状态。北京时间 22:22:14 的真实来信触发 Gmail 唤醒、ColorOS 解冻、广播放行和 `gmail-ls` Hans 同步放行；GMS 记录两条推送广播均投递成功，分别耗时 242 ms、328 ms，用户确认收到通知。这些数值是广播投递耗时，不是发信到通知展示的总延迟。

X 尚未单独做端到端推送测试。本版新增 API 37 适配，不宣称未来系统版本已验证。

## 安装与构建

下载 APK 与校验文件，在同一目录执行 `shasum -a 256 -c SHA256SUMS`。可直接覆盖安装 v2，无需卸载或重设白名单；保持系统框架作用域启用，然后重启并解锁。待原有约一分钟的开机初始化完成后，可在不手动打开 Gmail 的情况下发送新邮件复测，无需调整 HMA。

本次沿用 JDK 21、Android SDK 36、Gradle 8.13、Android Gradle Plugin 8.13.0；目标 SDK 和权限未变，API 37 适配通过运行时反射完成。

```sh
./gradlew :app:assembleDebug :app:lintDebug
java tests/BroadcastHookSelectionTest.java app/src/main/java/com/kooritea/fcmfix/xposed/BroadcastFix.java
```

JVM 测试检查入口选择、签名校验及回退，不替代手机上的实际推送测试。自行构建需使用原签名才能覆盖既有安装；重新构建的字节哈希不保证与本 Release 相同。
