package com.cznorth.barragepass;

import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 游戏助手（com.oplus.games 10.40.22）弹幕通知「点击穿透」。
 *
 * 弹幕窗口默认就是 NOT_TOUCHABLE；它靠 OplusGestureMonitorManager 全局监听指针事件，
 * 由 BarrageView$registerInputEvent$1.invoke(InputEvent) 判定「点中弹幕」后才停住并接管触摸。
 *
 * 开关复用游戏助手自带的「弹幕消息回复」「弹幕屏蔽」：点弹幕停下只是为了回复/屏蔽，
 * 两个都关掉时跳过上面的回调 → 弹幕照常飘、点击全部落到游戏；开了任一个就恢复原行为。
 * （GameBarrageFeature.isOpenBarrageReply / isOpenBarrageShield，Kotlin object，名字未混淆）
 */
public class Hook implements IXposedHookLoadPackage {
    private static final String GAMES = "com.oplus.games";
    private static final String CB =
            "com.oplus.feature.barragenotification.BarrageView$registerInputEvent$1";
    private static final String FEATURE =
            "com.oplus.feature.barragenotification.GameBarrageFeature";

    private static Object feature;
    private static Method isReply, isShield;

    /** 回复、屏蔽都关 → 穿透。查询失败时默认穿透（这是模块的本意）。 */
    static boolean passthrough(ClassLoader cl) {
        try {
            if (feature == null) {
                Class<?> c = cl.loadClass(FEATURE);
                isReply = c.getMethod("isOpenBarrageReply");
                isShield = c.getMethod("isOpenBarrageShield");
                feature = c.getField("INSTANCE").get(null);
            }
            return !(Boolean) isReply.invoke(feature) && !(Boolean) isShield.invoke(feature);
        } catch (Throwable t) {
            return true;
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!GAMES.equals(lpparam.packageName)) return;
        final String proc = lpparam.processName;
        final ClassLoader cl = lpparam.classLoader;
        try {
            XposedHelpers.findAndHookMethod(CB, cl, "invoke", "android.view.InputEvent",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam p) {
                            if (passthrough(cl)) p.setResult(null);
                        }
                    });
            XposedBridge.log("barragepass: input observer hooked in " + proc
                    + " (passthrough now=" + passthrough(cl) + ")");
        } catch (Throwable t) {
            XposedBridge.log("barragepass: input observer hook failed in " + proc + ": " + t);
        }
    }
}
