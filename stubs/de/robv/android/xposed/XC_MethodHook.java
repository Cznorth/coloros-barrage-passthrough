package de.robv.android.xposed;
public abstract class XC_MethodHook {
    public XC_MethodHook() {}
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {}
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {}
    public class Unhook {}
    public static final class MethodHookParam {
        public Object thisObject; public Object[] args;
        public void setResult(Object r) {}
    }
}
