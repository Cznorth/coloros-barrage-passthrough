# ColorOS 游戏助手 · 弹幕通知点击穿透

一个 LSPosed 模块。打游戏时游戏助手的**弹幕通知**飘过来，手指刚好点到它，弹幕就会停住并吃掉触摸，团战里技能、走位全被卡住。装上这个模块后，弹幕照常飘，**点击直接穿透到游戏**。

## 效果 / 开关

不新增设置项，直接复用游戏助手自带的两个开关（游戏内「游戏助手 → 弹幕通知」或「我的游戏性能 → 沉浸设置」均可）：

| 弹幕消息回复 | 弹幕屏蔽 | 点到弹幕时 |
|---|---|---|
| 关 | 关 | **穿透到游戏**，弹幕继续飘 |
| 任一开启 | | 原行为：弹幕停住，可回复 / 屏蔽 |

点弹幕停下本来就是为了回复或屏蔽；两个都不用时，停下来只会碍事。切换即时生效。

## 安装

前提：已 root，装好 [LSPosed](https://github.com/JingMatrix/LSPosed)（Zygisk 版即可）。

1. 从 [Releases](../../releases) 下载 `barrage-passthrough.apk` 安装
2. LSPosed → 模块 → 启用「弹幕通知点击穿透」，作用域勾选**游戏助手**（`com.oplus.games`）
3. 强行停止游戏助手（或重启手机）
4. 关掉「弹幕消息回复」和「弹幕屏蔽」

## 测试环境

- PLK110，ColorOS 17.0（Android 17），游戏助手 `com.oplus.games` 10.40.22
- LSPosed v2.2.1（KernelSU + Zygisk）
- 王者荣耀

其他机型 / 版本未测试。只要游戏助手里还有下面这个类，大概率能用；欢迎反馈。

## 原理

弹幕窗口本身是 `FLAG_NOT_TOUCHABLE` 的，并不直接接收触摸。`BarrageView` 通过
`OplusGestureMonitorManager` 注册了一个全局指针监听，在手指短按抬起时由
`com.oplus.feature.barragenotification.BarrageView$registerInputEvent$1.invoke(InputEvent)`
判断是否点中了弹幕，点中后才把弹幕停住、窗口改为可触摸（于是挡住了游戏）。

模块只 hook 这一个回调：当 `GameBarrageFeature.isOpenBarrageReply()` 和
`isOpenBarrageShield()` 都为 false 时直接 return，弹幕就永远不会进入「被点中」状态。
没有改窗口参数，也不碰游戏进程。

失效排查：LSPosed 日志里搜 `barragepass:`，能看到是否 hook 成功以及当前是否处于穿透状态。

## 自己构建

不需要 Gradle / Android Studio，只要 JDK 和 Android SDK（build-tools + platforms）：

```bash
ANDROID_HOME=~/Android/Sdk ./build.sh       # 产物 build/barrage-passthrough.apk
```

`stubs/` 里是编译用的 Xposed API 桩，不会打包进 APK。

## 免责

与 OPPO / 一加 / ColorOS 官方无关。使用 root 与 Xposed 有风险，后果自负。

## License

MIT
