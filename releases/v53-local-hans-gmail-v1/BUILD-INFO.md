# FCMFix Local — Hans Gmail v1

## 版本与来源

- 上游仓库：<https://github.com/kooritea/fcmfix>
- 上游基线：`b366825a1922c0f315efd4759f6c3eb58540dd74`
- 本地源码分支：`local/coloros16-hans-gmail`
- 发布标签：`v53-local-hans-gmail-v1`
- 包名：`com.kooritea.fcmfix.local`
- 应用名：`FCMFix Local (Hans Gmail)`
- versionCode：`5301`
- versionName：`53-b366825-local-hans-gmail-v1`
- 变体：`debug`；保留原有签名，不重新签名。
- APK v2 签名验证通过；签名证书 SHA-256：`0ce547348816a2d750e7c9786bb50d74c7d943cea8ec41bd1965672f4d34f236`。
- 既有 APK 文件时间：2026-08-08 22:01:28 +08:00。
- 文件大小：7,586,938 字节。
- SHA-256：`629ef4e2f805ec0d35ce789a3a57b21a7f527e203907267559b9b92b37a8ea6e`。

本次发布使用现成的 `app/build/outputs/apk/debug/app-debug.apk`，不是重新构建的替代文件。2026-10-10 从测试手机读取的已安装 APK 与该文件 SHA-256 相同。Release 附带 APK、`SHA256SUMS` 与本说明；GitHub 同时提供标签对应的源码归档。签名私钥、账号、设备配置和诊断原始日志不上传。

## 修改范围

1. 独立应用包名及对应配置更新、日志广播、界面入口引用，便于与官方版共存。
2. 在 `OplusSceneManager.checkJobIfRestricted` 中，仅当目标是模块白名单中的 Gmail、服务为系统 `SyncJobService`，且任务参数为 `provider=gmail-ls`、`owningPackage=com.google.android.gm` 时，返回不受 Hans 限制。
3. 保留上游广播唤醒、ColorOS 解冻、GMS 重连、通知保留和自启动限制处理代码；其实际启用仍取决于 LSPosed 作用域、模块选项及系统兼容性。

新增 Hans 任务放行只针对 Gmail，不是 X 或其他应用的通用同步任务补丁，也没有新增后台保活或定时轮询机制。上游提交 `b366825` 已移除 GMS 推荐作用域；本分支没有改变这一决定。

## 安装与配置

1. 下载同一 Release 的 APK 和 `SHA256SUMS`；在下载目录运行 `shasum -a 256 -c SHA256SUMS`。
2. 安装 APK，在支持 libxposed API 101 的 LSPosed 中启用 **FCMFix Local (Hans Gmail)**，按上游方式选择系统框架作用域，再重启使系统 hook 生效。
3. 打开本地版模块界面，将 Gmail、X 等需要放行的应用加入白名单，确认没有保存失败提示。官方版与本地版的配置不互通，不能相互代替。
4. 验证实际来信/消息是否依次经过 GMS 接收、应用唤醒/同步、通知展示。重装 LSPosed 或迁移 Root 环境后，应重新确认远程白名单仍然存在。

该包是 LSPosed APK，不是供 KSU / Magisk 刷入的 ZIP。自行重编译时需使用与既有安装一致的签名才能覆盖升级；本仓库不分发签名私钥。

## 验证边界与当前状态

2026-08-08 在 OnePlus PKX110 / ColorOS 16 的本地测试中，曾观察到 Gmail 被系统结束或重启后未手动打开时，收到推送、Hans 同步任务放行并生成通知。该结果仅代表当时的测试。

2026-10-10 在 KSU / LSPosed 环境重做后，确认模块已加载进系统，但当前 LSPosed 数据库中没有本地模块的远程白名单记录；GMS 当次检查已连接。漏通知/延迟的完整原因尚未完成端到端确认。本 Release 仅保存原实验版本，不包含该次回归的修复，不承诺跨系统、GMS、应用或框架升级后的持续兼容。

## 从源码构建

安装合适的 JDK 与 Android SDK 36，配置 `ANDROID_HOME` 或未跟踪的 `local.properties`，然后运行：

```sh
./gradlew assembleDebug
```

输出为 `app/build/outputs/apk/debug/app-debug.apk`。重新构建产物的签名和字节哈希不保证与归档 APK 一致。上游 GitHub Actions 工作流保留原样，依赖上游发布所用 Secrets；本次预编译资产通过手动 Release 发布，不依赖这些 Secrets。
